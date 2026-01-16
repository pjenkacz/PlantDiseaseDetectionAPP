import os

from fastapi import APIRouter, Depends, UploadFile, File, HTTPException, status
from sqlalchemy.orm import Session

from app.core.config import settings
from app.database.database import get_db
from app.domain import models, schemas
from app.core.security import get_current_user
from app.domain.yolo_service import run_yolo
from pillow_heif import read_heif
from PIL import Image
from pathlib import Path

router = APIRouter(
    prefix="/photos",
    tags=["photos"],
)


@router.post("/upload", response_model=schemas.PhotoUploadResponse)
async def upload_photo(
    photo: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user),
):
    filename = photo.filename
    if not filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No file uploaded",
        )

    ext = filename.rsplit(".", 1)[-1].lower()
    if ext not in settings.ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File extension .{ext} not allowed",
        )

    contents = await photo.read()
    if len(contents) > settings.MAX_FILE_SIZE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="File too large",
        )

    os.makedirs(os.path.join(settings.UPLOAD_DIR, "original"), exist_ok=True)
    os.makedirs(os.path.join(settings.UPLOAD_DIR, "processed"), exist_ok=True)

    base_name = os.path.basename(filename)

    # jeśli HEIC → zmieniamy rozszerzenie na JPG
    if ext == "heic":
        safe_name = f"photo_{current_user.id}_{base_name.rsplit('.', 1)[0]}.jpg"
        original_path = os.path.join(settings.UPLOAD_DIR, "original", safe_name)

        heif_file = read_heif(contents)
        image = Image.frombytes(
            heif_file.mode,
            heif_file.size,
            heif_file.data,
            "raw"
        )
        image.save(original_path, format="JPEG")
    else:
        safe_name = f"photo_{current_user.id}_{base_name}"
        original_path = os.path.join(settings.UPLOAD_DIR, "original", safe_name)

        with open(original_path, "wb") as f:
            f.write(contents)


    detections, processed_path = run_yolo(original_path)

    photo = models.Photo(
        user_id=current_user.id,
        original_path=original_path,
        processed_path=processed_path,
        detections_json=json.dumps(detections),
    )
    db.add(photo)
    db.commit()
    db.refresh(photo)

    rel_path = Path(processed_path).relative_to(settings.UPLOAD_DIR).as_posix()  # wymusza '/'
    url_path = f"/uploads/{rel_path}"

    return schemas.PhotoUploadResponse(
        photoId=str(photo.id),
        url=url_path,
        message="uploaded",
        detections=detections
    )

import json
from typing import List

@router.get("/get", response_model=schemas.PhotoUploadListResponse)
async def get_photos(
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    photos = (
        db.query(models.Photo)
        .filter(models.Photo.user_id == current_user.id)
        .all()
    )

    photo_items: List[schemas.PhotoItem] = []

    for photo in photos:
        if photo.detections_json:
            try:
                raw_detections = json.loads(photo.detections_json)
                if not isinstance(raw_detections, list):
                    raw_detections = []
            except ValueError:
                raw_detections = []
        else:
            raw_detections = []

        detections = [
            schemas.Detection(**det)
            for det in raw_detections
            if isinstance(det, dict)
        ]
        rel_path = Path(photo.processed_path).relative_to(settings.UPLOAD_DIR).as_posix()
        url_path = f"/uploads/{rel_path}"
        # 3. Budujemy PhotoItem
        photo_items.append(
            schemas.PhotoItem(
                id=photo.id,
                url=url_path,
                detections=detections
            )
        )

    return schemas.PhotoUploadListResponse(photos=photo_items)



