"""
test_photos.py — testy endpointów /photos/upload i /photos/get

KLUCZOWA TECHNIKA: MOCKOWANIE (unittest.mock.patch)
-----------------------------------------------------
run_yolo() ładuje model YOLOv11 z pliku .pt (~50MB) i robi inferencję na GPU/CPU.
W testach nie chcemy:
  - czekać 10 sekund na załadowanie modelu
  - wymagać pliku .pt w środowisku CI/CD
  - losowych wyników (testy muszą być deterministyczne)

patch("app.api.routers.photos.run_yolo") zamienia prawdziwą funkcję run_yolo
na atrapę (Mock), który zwraca co mu każemy. Tylko na czas trwania bloku `with`.

Analogia: zamiast uruchamiać prawdziwy samochód żeby sprawdzić czy deska rozdzielcza
świeci — podłączamy zasilacz i symulujemy że silnik działa.
"""

import io
from unittest.mock import patch

from PIL import Image


# ─── Helper: tworzy minimalny poprawny plik JPEG w pamięci ─────────────────────
def make_jpeg_bytes(width=100, height=100):
    """
    PIL (Pillow) tworzy obrazek w pamięci (BytesIO), nie na dysku.
    Dzięki temu testy nie pozostawiają plików na dysku.
    """
    img = Image.new("RGB", (width, height), color=(120, 80, 40))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf


# Stałe dane które "zwraca" nasz sfałszowany YOLO
MOCK_DETECTIONS = [
    {"class_id": 0, "confidence": 0.95, "box": [10.0, 20.0, 100.0, 200.0]}
]
MOCK_PROCESSED_PATH = "/tmp/slamleaf_test_uploads/processed/test_output.jpg"


# ═══════════════════════════════════════════════════════════════════
# /photos/get — pobieranie historii zdjęć
# ═══════════════════════════════════════════════════════════════════

def test_get_photos_requires_auth(client):
    """
    Sad path: próba pobrania historii bez tokenu.
    """
    response = client.get("/photos/get")
    assert response.status_code == 403


def test_get_photos_empty_for_new_user(client, registered_user):
    """
    Happy path: nowy user nie ma żadnych zdjęć — dostaje pustą listę.
    """
    response = client.get("/photos/get", headers=registered_user["headers"])

    assert response.status_code == 200
    data = response.json()
    assert "photos" in data
    assert data["photos"] == []


def test_get_photos_isolation_between_users(client, registered_user):
    """
    Test izolacji: zdjęcia usera A nie są widoczne dla usera B.
    """
    resp = client.post("/auth/register", json={
        "email":    "userb@example.com",
        "password": "pass123",
        "name":     "User B"
    })
    user_b_headers = {"Authorization": f"Bearer {resp.json()['token']}"}

    response = client.get("/photos/get", headers=user_b_headers)
    assert response.status_code == 200
    assert response.json()["photos"] == []


# ═══════════════════════════════════════════════════════════════════
# /photos/upload — wgrywanie zdjęcia
# ═══════════════════════════════════════════════════════════════════

def test_upload_requires_auth(client):
    """
    Sad path: upload bez tokenu → 403.
    """
    response = client.post(
        "/photos/upload",
        files={"photo": ("test.jpg", make_jpeg_bytes(), "image/jpeg")}
    )
    assert response.status_code == 403


def test_upload_invalid_extension_returns_400(client, registered_user):
    """
    Sad path: plik z niedozwolonym rozszerzeniem (.exe).
    Rozszerzenia na białej liście (ALLOWED_EXTENSIONS w config).
    """
    response = client.post(
        "/photos/upload",
        headers=registered_user["headers"],
        files={"photo": ("malware.exe", b"fake content", "application/octet-stream")}
    )
    assert response.status_code == 400


def test_upload_too_large_returns_400(client, registered_user):
    """
    Sad path: plik przekraczający MAX_FILE_SIZE (10MB).

    """
    big_file = b"x" * (11 * 1024 * 1024)  # 11 MB

    response = client.post(
        "/photos/upload",
        headers=registered_user["headers"],
        files={"photo": ("big.jpg", big_file, "image/jpeg")}
    )
    assert response.status_code == 400


def test_upload_success_with_mocked_yolo(client, registered_user):
    """
    Happy path: poprawny upload zdjęcia z zamockowanym YOLO.
    Mockowanie do zastąpienia działania inferencji funkcji run_yolo()
    """
    with patch("app.api.routers.photos.run_yolo") as mock_yolo:
        mock_yolo.return_value = (MOCK_DETECTIONS, MOCK_PROCESSED_PATH)

        response = client.post(
            "/photos/upload",
            headers=registered_user["headers"],
            files={"photo": ("plant.jpg", make_jpeg_bytes(), "image/jpeg")}
        )

    assert response.status_code == 200
    data = response.json()

    assert "photoId" in data
    assert "url" in data
    assert "detections" in data
    assert len(data["detections"]) == 1
    assert data["detections"][0]["class_id"] == 0
    assert data["detections"][0]["confidence"] == 0.95


def test_upload_photo_appears_in_history(client, registered_user):
    """
    Test integracyjny: wgrane zdjęcie pojawia się w historii /photos/get.
    """
    # Krok 1: wgrywamy zdjęcie
    with patch("app.api.routers.photos.run_yolo") as mock_yolo:
        mock_yolo.return_value = (MOCK_DETECTIONS, MOCK_PROCESSED_PATH)

        upload_resp = client.post(
            "/photos/upload",
            headers=registered_user["headers"],
            files={"photo": ("leaf.jpg", make_jpeg_bytes(), "image/jpeg")}
        )

    assert upload_resp.status_code == 200
    uploaded_id = upload_resp.json()["photoId"]

    # Krok 2: pobieramy historię i sprawdzamy że zdjęcie tam jest
    history_resp = client.get("/photos/get", headers=registered_user["headers"])
    assert history_resp.status_code == 200

    photos = history_resp.json()["photos"]
    assert len(photos) == 1
    assert str(photos[0]["id"]) == uploaded_id


def test_upload_photos_not_shared_between_users(client, registered_user):
    """
    Test bezpieczeństwa: zdjęcia jednego usera nie są widoczne dla drugiego.
    """
    # User A wgrywa zdjęcie
    with patch("app.api.routers.photos.run_yolo") as mock_yolo:
        mock_yolo.return_value = (MOCK_DETECTIONS, MOCK_PROCESSED_PATH)
        client.post(
            "/photos/upload",
            headers=registered_user["headers"],
            files={"photo": ("leaf.jpg", make_jpeg_bytes(), "image/jpeg")}
        )

    # User B rejestruje się i pobiera historię
    resp = client.post("/auth/register", json={
        "email":    "userb@example.com",
        "password": "pass123",
        "name":     "User B"
    })
    user_b_headers = {"Authorization": f"Bearer {resp.json()['token']}"}

    history = client.get("/photos/get", headers=user_b_headers)
    assert history.json()["photos"] == []
