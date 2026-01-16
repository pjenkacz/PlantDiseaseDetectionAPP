from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.database import get_db
from app.domain import models, schemas
from app.core.security import (
    hash_password,
    verify_password,
    create_access_token,
    get_current_user,
)
router = APIRouter(
    prefix="/auth",
    tags=["auth"],
)

@router.post("/register", response_model=schemas.RegisterResponse)
def register(
    data: schemas.RegisterRequest,
    db: Session = Depends(get_db),
):
    # czy email już istnieje?
    existing = db.query(models.User).filter(models.User.email == data.email).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered",
        )

    password_hash = hash_password(data.password)

    # tworzymy usera
    user = models.User(
        email=data.email,
        password_hash=password_hash,
        name=data.name,
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    # generujemy token
    token = create_access_token(subject=str(user.id))

    return schemas.RegisterResponse(
        token=token,
        userId=str(user.id),
        message="Registered successfully",
    )


@router.post("/login", response_model=schemas.LoginResponse)
def login(
    data: schemas.LoginRequest,
    db: Session = Depends(get_db),
):
    user = db.query(models.User).filter(models.User.email == data.email).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
        )

    if not verify_password(data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
        )
    token = create_access_token(subject=str(user.id))

    return schemas.LoginResponse(
        token=token,
        userId=str(user.id),
        message="Logged in successfully",
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