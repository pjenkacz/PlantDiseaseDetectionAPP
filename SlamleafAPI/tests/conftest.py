"""
conftest.py — plik konfiguracyjny pytest, wczytywany automatycznie przed testami.

DLACZEGO os.environ NA SAMEJ GÓRZE?
-------------------------------------
Twoja aplikacja używa `pydantic-settings` z @lru_cache:
    settings = get_settings()  <-- wywołane przy pierwszym imporcie config.py

lru_cache = "zapamiętaj wynik i nie wołaj funkcji drugi raz".
Jeśli ustawimy zmienne środowiskowe ZA PÓŹNO (po imporcie), settings już będą
z pliku .env (czyli wskazywać na MySQL). Musimy podmienić je PRZED importem aplikacji.

DLACZEGO SQLite zamiast MySQL?
---------------------------------
MySQL wymaga działającego serwera. SQLite to zwykły plik — zero konfiguracji,
działa w pamięci lub jako plik tymczasowy. Idealne do testów.
"""

import os

# ── Nadpisujemy zmienne środowiskowe PRZED jakimkolwiek importem aplikacji ──────
os.environ["DATABASE_URL"]   = "sqlite:///./test_slamleaf.db"
os.environ["SECRET_KEY"]     = "test-secret-key-only-for-tests"
os.environ["UPLOAD_DIR"]     = "/tmp/slamleaf_test_uploads"
os.environ["YOLO_MODEL_PATH"] = "models/best.pt"  # nie używany (mockowany w testach)

# StaticFiles w main.py wymaga, żeby katalog uploads/ istniał przy starcie aplikacji
os.makedirs("/tmp/slamleaf_test_uploads/original",  exist_ok=True)
os.makedirs("/tmp/slamleaf_test_uploads/processed", exist_ok=True)

# ── Importy aplikacji dopiero PO ustawieniu środowiska ──────────────────────────
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.database.database import Base, get_db
from main import app

# ── Silnik SQLite dla testów ─────────────────────────────────────────────────────
# check_same_thread=False: SQLite domyślnie blokuje wielowątkowe użycie,
# ale pytest może wołać ten sam silnik z różnych miejsc — wyłączamy blokadę.
SQLALCHEMY_TEST_URL = "sqlite:///./test_slamleaf.db"
engine = create_engine(
    SQLALCHEMY_TEST_URL,
    connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


# ── FIXTURE: setup_database ──────────────────────────────────────────────────────
# autouse=True: działa dla KAŻDEGO testu automatycznie, bez wołania jej z imienia.
# Wzorzec yield: kod przed yield = setup, kod po yield = teardown.
# Efekt: każdy test dostaje CZYSTĄ, pustą bazę. Zero zależności między testami.
@pytest.fixture(autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)   # tworzy tabele
    yield
    Base.metadata.drop_all(bind=engine)     # usuwa tabele po teście


# ── FIXTURE: client ───────────────────────────────────────────────────────────────
# TestClient to klient HTTP działający w pamięci — symuluje requesty bez sieci.
# dependency_overrides: mechanizm FastAPI do podmiany zależności w testach.
# Podmieniamy get_db (zwracającą sesję MySQL) na naszą sesję SQLite.
@pytest.fixture
def client(setup_database):
    def override_get_db():
        db = TestingSessionLocal()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db
    yield TestClient(app)
    app.dependency_overrides.clear()  # sprzątamy po teście


# ── FIXTURE: registered_user ─────────────────────────────────────────────────────
# Fixture zwracająca gotowego, zarejestrowanego użytkownika z tokenem.
# Używana w testach, które wymagają zalogowania — żeby nie powtarzać rejestracji
# w każdym teście osobno.
@pytest.fixture
def registered_user(client):
    response = client.post("/auth/register", json={
        "email": "test@example.com",
        "password": "password123",
        "name": "Test User"
    })
    data = response.json()
    return {
        "email":    "test@example.com",
        "password": "password123",
        "name":     "Test User",
        "token":    data["token"],
        "userId":   data["userId"],
        # Gotowy nagłówek Authorization do wklejenia w każdy request:
        "headers":  {"Authorization": f"Bearer {data['token']}"},
    }
