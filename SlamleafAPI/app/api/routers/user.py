from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.database import get_db
from app.domain import models, schemas
from app.domain.schemas import EmailRequest, NameRequest, PasswordRequest
from app.core.security import (
    get_current_user,
    verify_password, hash_password
)

router = APIRouter(
    prefix="/user",
    tags=["user"],
)

@router.get("/me", response_model=schemas.UserRead)
def read_me(
    current_user: models.User = Depends(get_current_user)
):
    return schemas.UserRead(
        id=current_user.id,
        email=current_user.email,
        name=current_user.name,
    )

@router.put(
    "/changeEmail",
    status_code=status.HTTP_204_NO_CONTENT
)
def change_email(
    data: EmailRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    # sprawdzenie hasła

    if not verify_password(data.password, current_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED
        )

    # sprawdzenie czy email już istnieje
    existing = (
        db.query(models.User)
        .filter(models.User.email == data.email)
        .first()
    )
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT
        )
    current_user.email = data.email
    db.commit()

    return None

@router.put(
    "/changeName",
    status_code=status.HTTP_204_NO_CONTENT
)
def change_name(
    data: NameRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    if not verify_password(data.password, current_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED
        )

    current_user.name = data.name
    db.commit()

    return None

@router.put(
    "/changePassword",
    status_code=status.HTTP_204_NO_CONTENT
)
def change_password(
    data: PasswordRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    # sprawdzenie hasła
    if not verify_password(data.password, current_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED
        )

    new_password = hash_password(data.newPassword)
    current_user.password_hash = new_password
    db.commit()
    return None