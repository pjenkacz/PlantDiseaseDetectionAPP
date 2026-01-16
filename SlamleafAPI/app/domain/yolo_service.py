import os

from ultralytics import YOLO
from app.core.config import settings

model = YOLO(settings.YOLO_MODEL_PATH)
def run_yolo(image_path: str) -> tuple[list[dict], str]:

    results = model.predict(
        image_path,
        save=True,
        project=settings.UPLOAD_DIR,
        name="processed",
        exist_ok=True
    )
    r = results[0]
    processed_dir = r.save_dir
    filename = os.path.basename(image_path)
    processed_path = os.path.join(processed_dir, filename)
    # detekcje do JSON
    detections: list[dict] = []
    for box in r.boxes:
        cls_id = int(box.cls[0])
        conf = float(box.conf[0])
        x1, y1, x2, y2 = box.xyxy[0]
        detections.append({
            "class_id": cls_id,
            "confidence": conf,
            "box": [float(x1), float(y1), float(x2), float(y2)]
        })

    return detections, processed_path
