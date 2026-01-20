"""
test_user.py — testy endpointów /user/me, /user/changeName,
               /user/changeEmail, /user/changePassword

WZORZEC W TYCH TESTACH:
------------------------
Każdy endpoint zarządzania kontem wymaga:
  1. Aktywnego tokenu JWT (jesteś zalogowany)
  2. Aktualnego hasła jako potwierdzenia tożsamości (dodatkowa warstwa bezp.)

Dlatego prawie każdy test używa fixture `registered_user`,
który daje nam gotowego usera + token.
"""


# ═══════════════════════════════════════════════════════════════════
# /user/me — pobranie danych profilu
# ═══════════════════════════════════════════════════════════════════

def test_get_user_me(client, registered_user):
    """
    Happy path: zalogowany user pobiera swój profil.

    /user/me (z routera user.py) i /auth/me robią to samo —
    testujemy oba żeby upewnić się, że user_router jest poprawnie zarejestrowany.
    To test który weryfikuje FIX który właśnie zrobiliśmy w main.py
    (brakujący user_router).
    """
    response = client.get("/user/me", headers=registered_user["headers"])

    assert response.status_code == 200
    data = response.json()
    assert data["email"] == registered_user["email"]
    assert data["name"]  == registered_user["name"]
    assert "id" in data


# ═══════════════════════════════════════════════════════════════════
# /user/changeName
# ═══════════════════════════════════════════════════════════════════

def test_change_name_success(client, registered_user):
    """
    Happy path: zmiana nazwy użytkownika.

    Endpoint zwraca 204 No Content — sukces bez ciała odpowiedzi.
    Weryfikujemy zmianę wykonując GET /user/me po operacji.

    DLACZEGO 204 a nie 200?
    Konwencja REST: 200 gdy zwracamy dane, 204 gdy operacja się udała
    ale nie ma nic do zwrócenia.
    """
    response = client.put("/user/changeName", json={
        "name":     "New Name",
        "password": registered_user["password"]
    }, headers=registered_user["headers"])

    assert response.status_code == 204

    # Weryfikacja: sprawdzamy czy zmiana faktycznie trafiła do bazy
    me = client.get("/user/me", headers=registered_user["headers"])
    assert me.json()["name"] == "New Name"


def test_change_name_wrong_password_returns_401(client, registered_user):
    """
    Sad path: błędne hasło przy zmianie nazwy.

    Twój kod w user.py wywołuje verify_password() przed każdą zmianą.
    Bez tego ktoś mógłby zmienić nazwę użytkownika kradnąc jego sesję.
    """
    response = client.put("/user/changeName", json={
        "name":     "Hacker Name",
        "password": "wrongpassword"
    }, headers=registered_user["headers"])

    assert response.status_code == 401


def test_change_name_without_token_returns_403(client):
    """
    Sad path: brak tokenu — middleware blokuje request zanim dotrze do endpointu.
    """
    response = client.put("/user/changeName", json={
        "name":     "Ghost",
        "password": "irrelevant"
    })

    assert response.status_code == 403


# ═══════════════════════════════════════════════════════════════════
# /user/changeEmail
# ═══════════════════════════════════════════════════════════════════

def test_change_email_success(client, registered_user):
    """
    Happy path: zmiana emaila na nowy, wolny adres.
    """
    response = client.put("/user/changeEmail", json={
        "email":    "newemail@example.com",
        "password": registered_user["password"]
    }, headers=registered_user["headers"])

    assert response.status_code == 204

    # Weryfikacja: nowy email widoczny w profilu
    me = client.get("/user/me", headers=registered_user["headers"])
    assert me.json()["email"] == "newemail@example.com"


def test_change_email_to_taken_email_returns_409(client, registered_user):
    """
    Sad path: próba zmiany emaila na taki który już istnieje w systemie.

    409 Conflict = zasób już istnieje. Twój kod sprawdza to
    zapytaniem do bazy przed zmianą. To ważne — bez tego można by
    "przejąć" konto innego użytkownika.

    SETUP: rejestrujemy drugiego usera żeby zająć email.
    """
    # Rejestrujemy drugiego usera z innym emailem
    client.post("/auth/register", json={
        "email":    "taken@example.com",
        "password": "pass123",
        "name":     "Second User"
    })

    # Pierwszy user próbuje zmienić email na zajęty
    response = client.put("/user/changeEmail", json={
        "email":    "taken@example.com",
        "password": registered_user["password"]
    }, headers=registered_user["headers"])

    assert response.status_code == 409


def test_change_email_wrong_password_returns_401(client, registered_user):
    """Sad path: błędne hasło przy zmianie emaila."""
    response = client.put("/user/changeEmail", json={
        "email":    "hacker@example.com",
        "password": "wrongpassword"
    }, headers=registered_user["headers"])

    assert response.status_code == 401


def test_change_email_invalid_format_returns_422(client, registered_user):
    """
    Sad path: email w nieprawidłowym formacie.

    Twój schemat EmailRequest używa `EmailStr` z Pydantic —
    automatyczna walidacja formatu emaila bez żadnego kodu z twojej strony.
    """
    response = client.put("/user/changeEmail", json={
        "email":    "notanemail",
        "password": registered_user["password"]
    }, headers=registered_user["headers"])

    assert response.status_code == 422


# ═══════════════════════════════════════════════════════════════════
# /user/changePassword
# ═══════════════════════════════════════════════════════════════════

def test_change_password_success(client, registered_user):
    """
    Happy path: zmiana hasła na nowe.

    Po zmianie hasła weryfikujemy że:
    1. Stare hasło już NIE działa (login zwraca 401)
    2. Nowe hasło działa poprawnie (login zwraca 200)

    To pełna weryfikacja end-to-end — nie tylko że endpoint zwraca 204,
    ale że zmiana faktycznie zadziałała w bazie.
    """
    response = client.put("/user/changePassword", json={
        "password":    registered_user["password"],
        "newPassword": "brandnewpassword456"
    }, headers=registered_user["headers"])

    assert response.status_code == 204

    # Stare hasło nie powinno już działać
    old_login = client.post("/auth/login", json={
        "email":    registered_user["email"],
        "password": registered_user["password"]
    })
    assert old_login.status_code == 401

    # Nowe hasło powinno działać
    new_login = client.post("/auth/login", json={
        "email":    registered_user["email"],
        "password": "brandnewpassword456"
    })
    assert new_login.status_code == 200


def test_change_password_wrong_current_returns_401(client, registered_user):
    """
    Sad path: podanie złego aktualnego hasła przy zmianie.

    Bez tej walidacji ktoś z wykradzionym tokenem mógłby zablokować
    prawdziwego właściciela konta zmieniając mu hasło.
    """
    response = client.put("/user/changePassword", json={
        "password":    "wrongcurrentpassword",
        "newPassword": "newpassword123"
    }, headers=registered_user["headers"])

    assert response.status_code == 401
