from datetime import datetime, timedelta
from typing import Optional

import bcrypt
from jose import JWTError, jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session

from app.core.config import settings
from app.database.database import get_db
from app.domain import models


# ===== HASŁA (bcrypt) =====

def hash_password(password: str) -> str:
    """Zamiana hasła na hash bcrypt."""
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password.encode("utf-8"), salt)
    return hashed.decode("utf-8")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Sprawdzenie, czy hasło użytkownika pasuje do hasha."""
    return bcrypt.checkpw(
        plain_password.encode("utf-8"),
        hashed_password.encode("utf-8")
    )

# ===== JWT =====
def create_access_token(
    subject: str,
    expires_delta: Optional[timedelta] = None
) -> str:
    """
    Tworzy JWT zawierający np. user_id w polu 'sub'.
    """
    if expires_delta is None:
        expires_delta = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)

    expire = datetime.utcnow() + expires_delta

    to_encode = {
        "sub": subject,          # np. user_id jako string
        "exp": expire
    }

    encoded_jwt = jwt.encode(
        to_encode,
        settings.SECRET_KEY,
        algorithm=settings.ALGORITHM
    )
    return encoded_jwt


def decode_access_token(token: str) -> dict:
    """
    Dekoduje i weryfikuje JWT. Rzuca exception przy błędnym / wygasłym tokenie.
    """
    try:
        payload = jwt.decode(
            token,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM]
        )
        return payload
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
            headers={"WWW-Authenticate": "Bearer"},
        )


# ===== DEPENDENCY DO AUTORYZACJI (Bearer token) =====
bearer_scheme = HTTPBearer(auto_error=True)

"""
Dependency: pobiera użytkownika na podstawie Bearer tokena.
"""
def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> models.User:
    # Dependency: pobiera użytkownika na podstawie Bearer tokena.
    token = credentials.credentials
    payload = decode_access_token(token)
    user_id: str = payload.get("sub")
    if user_id is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token missing subject",
        )
    user = db.query(models.User).filter(models.User.id == int(user_id)).first()
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
        )
    return user
