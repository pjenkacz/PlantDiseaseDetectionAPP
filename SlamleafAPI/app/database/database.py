from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from app.core.config import settings

# Silnik bazy danych
engine = create_engine(
    settings.DATABASE_URL,
    pool_pre_ping=True,
    echo=False
)
# Fabryka sesji
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
# Klasa bazowa dla modeli
Base = declarative_base()
# Dependency do uzyskania sesji w endpointach
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()