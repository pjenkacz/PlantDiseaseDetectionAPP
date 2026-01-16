from typing import Optional, List
from pydantic import BaseModel, EmailStr

# ===== AUTH – REQUESTY OD ANDROIDA =====
class RegisterRequest(BaseModel):
    email: str
    password: str
    name: str

class LoginRequest(BaseModel):
    email: str
    password: str

# ===== AUTH – ODPOWIEDZI DO ANDROIDA =====
class LoginResponse(BaseModel):
    token: str
    userId: str
    message: Optional[str] = None


class RegisterResponse(BaseModel):
    token: str
    userId: str
    message: Optional[str] = None

# ===== PHOTO – ODPOWIEDŹ PO UPLOADZIE  =====
class ApiError(BaseModel):
    error: str
    message: str

# ===== UŻYTKOWNIK – WEWNĘTRZNE SCHEMATY =====
class UserBase(BaseModel):
    email: str
    name: str

class UserCreate(UserBase):
    password: str

class UserRead(BaseModel):
    id: int
    email: str
    name: str

class EmailRequest(BaseModel):
    email: EmailStr
    password: str

class NameRequest(BaseModel):
    name: str
    password: str

class PasswordRequest(BaseModel):
    password: str
    newPassword: str

class Detection(BaseModel):
    class_id: int
    confidence: float
    box: List[float]


class PhotoItem(BaseModel):
    id: int
    url: str
    detections: List[Detection]

class PhotoUploadResponse(BaseModel):
    photoId: str
    url: str
    message: str
    detections: list[Detection]

class PhotoUploadListResponse(BaseModel):
    photos: List[PhotoItem]