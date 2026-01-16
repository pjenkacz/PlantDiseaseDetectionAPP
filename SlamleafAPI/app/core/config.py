from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    DATABASE_URL: str
    SECRET_KEY: str
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    # Server
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    UPLOAD_DIR: str = "uploads"
    YOLO_MODEL_PATH: str = "models/best.pt"

    MAX_FILE_SIZE: int = 10 * 1024 * 1024  # 10 MB
    ALLOWED_EXTENSIONS: set = {"jpg", "jpeg", "png" , "heic"}

    class Config:
        env_file = ".env"

@lru_cache()
def get_settings():
    return Settings()

settings = get_settings()