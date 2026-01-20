"""
test_auth.py — testy endpointów /auth/register i /auth/login

JAK DZIAŁA pytest?
-------------------
pytest szuka funkcji zaczynających się od `test_`.
Każda taka funkcja to osobny test. Jeśli rzuci AssertionError → test FAILED.
Fixtures (np. `client`) są wstrzykiwane automatycznie przez pytest po nazwie parametru.

CO TESTUJEMY?
-------------
Tzw. "happy path" (wszystko idzie dobrze) i "sad path" (błędne dane, edge cases).
Rekruter zapyta: "jak zapewniasz że twój auth działa?" — tu jest odpowiedź.
"""


# ═══════════════════════════════════════════════════════════════════
# REJESTRACJA
# ═══════════════════════════════════════════════════════════════════

def test_register_success(client):
    """
    Happy path: nowy użytkownik rejestruje się poprawnie.
    """
    response = client.post("/auth/register", json={
        "email":    "newuser@example.com",
        "password": "strongpassword",
        "name":     "New User"
    })

    assert response.status_code == 200
    data = response.json()
    assert "token" in data
    assert len(data["token"]) > 0      # token nie jest pustym stringiem
    assert "userId" in data


def test_register_duplicate_email_returns_400(client):
    """
    Sad path: próba rejestracji z emailem który już istnieje.
    """
    payload = {
        "email":    "duplicate@example.com",
        "password": "pass123",
        "name":     "First User"
    }
    client.post("/auth/register", json=payload)

    # Druga rejestracja tym samym emailem
    response = client.post("/auth/register", json={
        "email":    "duplicate@example.com",
        "password": "otherpass",
        "name":     "Second User"
    })

    assert response.status_code == 400
    assert "already registered" in response.json()["detail"]


def test_register_missing_fields_returns_422(client):
    """
    Sad path: brakujące pola w rejestracji.
    422 Unprocessable Entity to kod zwracany przez FastAPI/Pydantic
    """
    response = client.post("/auth/register", json={
        "email": "noemail@example.com"
        # brak password i name
    })

    assert response.status_code == 422


# ═══════════════════════════════════════════════════════════════════
# LOGOWANIE
# ═══════════════════════════════════════════════════════════════════

def test_login_success(client, registered_user):
    """
    Happy path: logowanie z poprawnymi danymi.
    """
    response = client.post("/auth/login", json={
        "email":    registered_user["email"],
        "password": registered_user["password"]
    })

    assert response.status_code == 200
    data = response.json()
    assert "token" in data
    assert len(data["token"]) > 0
    assert data["userId"] == registered_user["userId"]


def test_login_wrong_password_returns_401(client, registered_user):
    """
    Sad path: błędne hasło → 401 Unauthorized.
    """
    response = client.post("/auth/login", json={
        "email":    registered_user["email"],
        "password": "wrongpassword"
    })
    assert response.status_code == 401


def test_login_nonexistent_email_returns_401(client):
    """
    Sad path: email który nie istnieje w bazie.
    WAŻNE: zwracamy 401 (nie 404!)
    """
    response = client.post("/auth/login", json={
        "email":    "nobody@example.com",
        "password": "whatever"
    })

    assert response.status_code == 401


# ═══════════════════════════════════════════════════════════════════
# ENDPOINT /auth/me — pobranie danych zalogowanego usera
# ═══════════════════════════════════════════════════════════════════

def test_get_me_authenticated(client, registered_user):
    """
    Happy path: zalogowany user pobiera swoje dane.
    """
    response = client.get("/auth/me", headers=registered_user["headers"])

    assert response.status_code == 200
    data = response.json()
    assert data["email"] == registered_user["email"]
    assert data["name"]  == registered_user["name"]


def test_get_me_without_token_returns_403(client):
    """
    Sad path: brak tokenu w nagłówku → 403 Forbidden.
    FastAPI używa HTTPBearer — jeśli nagłówek Authorization jest pusty,
    middleware zwraca 403 zanim request w ogóle dotrze do endpointu.
    """
    response = client.get("/auth/me")

    assert response.status_code == 403


def test_get_me_with_invalid_token_returns_401(client):
    """
    Sad path: token jest obecny, ale nieprawidłowy.
    Twój kod w security.py używa jwt.decode() — przy błędnym tokenie
    jose rzuca JWTError, którą łapiesz i zamieniasz na HTTP 401.
    """
    response = client.get(
        "/auth/me",
        headers={"Authorization": "Bearer thisisnotavalidtoken"}
    )

    assert response.status_code == 401
