from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.auth import router as auth_router
from app.database import Base, engine
from fastapi.staticfiles import StaticFiles
from app.api.routers.photos import router as photos_router
from app.core.config import settings

app = FastAPI(
    title="SlamLeaf Disease Detection API",
    description="API for detecting diseases in various plants using YOLOv11",
    version="1.0.0"
)

# CORS - pozwala aplikacji Android łączyć się z API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # W produkcji zmień na konkretne domeny!
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    return {
        "message": "Tomato Potato Disease Detection API",
        "status": "running",
        "version": "1.0.0"
    }

@app.get("/health")
async def health_check():
    return {"status": "healthy"}

Base.metadata.create_all(bind=engine)
app.include_router(auth_router)
app.include_router(auth_router)
app.include_router(photos_router)

# serwowanie plików z katalogu uploads/
app.mount(
    "/uploads",
    StaticFiles(directory=settings.UPLOAD_DIR),
    name="uploads"
)
