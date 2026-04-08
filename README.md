# Mobile Application for Plant Disease Recogniti on Using Machine Learning

> An Android mobile application that detects and classifies plant diseases 
> from photos using a YOLOv11 object detection model, 
> and recommends appropriate plant protection products available on the Polish market.

![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=flat&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-0.100+-009688?style=flat&logo=fastapi&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-7.0+-3DDC84?style=flat&logo=android&logoColor=white)
![YOLOv11](https://img.shields.io/badge/YOLOv11-Ultralytics-00FFFF?style=flat)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=flat&logo=mysql&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat)

---

## Table of Contents

- [About](#-about)
- [Architecture](#️-architecture)
- [Screenshots](#-screenshots)
- [Getting Started](#-getting-started)
- [ML Model](#-ml-model)
- [API Reference](#-api-reference)
- [License](#-license)

---

## 🌱 About

This project is a complete **plant disease detection system** built on a 
client-server architecture, developed as an engineering thesis at AGH 
University of Science and Technology (2026).

A **YOLOv11 object detection model** (trained via transfer learning on the 
PlantDoc dataset) runs on a Python/FastAPI backend, analyzing photos uploaded 
from an Android mobile app. The system not only identifies plant diseases, but 
also **recommends specific plant protection products** available on the Polish 
market, based on the official government registry.

As part of the research, a comparative analysis of single-stage (YOLOv11) and 
two-stage (Faster R-CNN) detection architectures was conducted. Model 
performance was validated on a **custom real-world dataset** of field 
photographs, confirming the model's ability to generalize beyond controlled 
lab conditions.

### Key Features
- 📸 Photo capture via camera or gallery upload
- 🔍 Automatic disease detection with confidence score
- 💊 Plant protection product recommendations (Polish market)
- 📜 Per-user analysis history
- 🔐 JWT-based authentication

## 🛠️ Tech Stack

| Layer      | Technology                          |
|------------|-------------------------------------|
| Mobile     | Android (Kotlin), Retrofit2, CameraX|
| Backend    | Python 3.11, FastAPI                |
| ML         | YOLOv11m (Ultralytics), PyTorch     |
| Database   | MySQL 8.4, SQLAlchemy               |
| Auth       | JWT (Bearer token)                  |

---
## 🏗️ Architecture

The system follows a client-server architecture where all computation 
is handled server-side, keeping the Android client lightweight.

![Architecture diagram](https://github.com/user-attachments/assets/348652e4-d1ef-436c-9f3a-a5e634e964e8)

The Android app sends a photo to the FastAPI backend over HTTP, where it is 
passed to the YOLOv11 model for inference. The model localizes disease symptoms 
and returns bounding boxes with class labels and confidence scores. Both the 
original and annotated images are saved on the server and linked to the user's 
account in MySQL. The client receives the processed image along with disease 
details and recommended plant protection products.

All endpoints except login and register are JWT-protected — users can only 
access their own data.

---

## 📱 Screenshots

<div align="center">

<table>
  <tr>
    <td align="center"><b>Scan</b></td>
    <td align="center"><b>History</b></td>
    <td align="center"><b>Profile</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/scanPanel.png" width="220"/></td>
    <td><img src="docs/screenshots/historyPanel.png" width="220"/></td>
    <td><img src="docs/screenshots/profilePanel.png" width="220"/></td>
  </tr>
  <tr>
    <td align="center">Take a photo or pick one from gallery<br>to start disease analysis</td>
    <td align="center">Browse all previous analyses with<br>detected disease and confidence score</td>
    <td align="center">Manage account details, change<br>name, email or password</td>
  </tr>
</table>

<br>

<table>
  <tr>
    <td align="center" colspan="2"><b>Disease Analysis Detail</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/analiza1.png" width="220"/></td>
    <td><img src="docs/screenshots/analiza2.png" width="220"/></td>
  </tr>
  <tr>
    <td colspan="2" align="center">View annotated image, disease description and recommended plant protection products</td>
  </tr>
</table>

</div>

## 🚀 Getting Started

### Prerequisites
- Python 3.11+
- Android Studio
- MySQL 8.4
- Git

### Backend Setup
```bash
git clone https://github.com/pjenkacz/PlantDiseaseDetectionAPP.git
cd backend
pip install -r requirements.txt
```

Create a `.env` file in the backend root directory:
```env
DATABASE_URL=mysql+pymysql://user:password@localhost:3306/dbname
SECRET_KEY=your_secret_key
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE=60
HOST=0.0.0.0
PORT=8000
```
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### Android App Setup

1. Open the `android/` folder in Android Studio
2. In `RetrofitClient.kt` set `BASE_URL` to your server address:
```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:8000/"
```
3. Run the app on a physical device or emulator
---

## 🤖 ML Model

The model development involved training and evaluating multiple architectures 
on the **PlantDoc dataset** (2,390 images, 30 disease classes) using transfer 
learning from COCO-pretrained weights. Both single-stage (YOLOv11) and 
two-stage (Faster R-CNN) detection architectures were compared.

| Model | Precision | Recall | F1 | mAP@50 | mAP@50-95 |
|-------|-----------|--------|----|--------|-----------|
| YOLOv11n | 0.630 | 0.607 | 0.618 | 0.634 | 0.447 |
| YOLOv11s | 0.686 | 0.632 | 0.657 | 0.674 | 0.505 |
| **YOLOv11m** | **0.734** | **0.683** | **0.708** | **0.732** | **0.546** |
| Faster R-CNN R50-FPN | — | — | — | 0.605 | 0.458 |

YOLOv11m was selected as the best-performing variant and further evaluated 
with data augmentation (geometric and photometric transformations) to assess 
its impact on generalization.

| Augmentation | Precision | Recall | F1 | mAP@50 | mAP@50-95 |
|-------------|-----------|--------|----|--------|-----------|
| Low | 0.7303 | 0.6413 | 0.6829 | 0.6992 | 0.5003 |
| Medium | 0.6667 | 0.6730 | 0.6698 | 0.7070 | 0.5192 |
| **High** | **0.7406** | **0.6506** | **0.6927** | **0.7228** | **0.5293** |

Augmentation did not significantly improve over the baseline YOLOv11m. **YOLOv11m without augmentation** was selected for 
deployment in the application.


## API Reference

All endpoints except `/auth/register` and `/auth/login` require a Bearer token 
in the `Authorization` header.

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | No | Register a new user account |
| POST | `/auth/login` | No | Login and retrieve JWT access token |
| POST | `/photos/upload` | Yes | Upload a plant photo and run disease detection |
| GET | `/photos/get` | Yes | Retrieve all photos and detection results for the authenticated user |
| GET | `/user/me` | Yes | Get current authenticated user details |
| PUT | `/user/changeEmail` | Yes | Update user email address |
| PUT | `/user/changeName` | Yes | Update user display name |
| PUT | `/user/changePassword` | Yes | Update user password |

---

## License

This project is licensed under the MIT License — feel free to use, modify 
and distribute it as you wish.
