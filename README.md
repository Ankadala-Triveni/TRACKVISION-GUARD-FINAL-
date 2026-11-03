# TRACKVISION GUARD — Railway Track Defect Detection & Safety Monitoring System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.14-blue.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688.svg)](https://fastapi.tiangolo.com/)
[![YOLO11](https://img.shields.io/badge/YOLO11-Ultralytics-red.svg)](https://github.com/ultralytics/ultralytics)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-Proprietary-yellow.svg)]()

> **Enterprise-grade, AI-powered railway track inspection, real-time defect segmentation, automated severity risk scoring, geospatial telemetry mapping, and maintenance lifecycle dispatch intelligence.**

---

## 📌 Executive Summary

**TRACKVISION GUARD** is a complete, full-stack railway track safety platform designed for Indian Railways and modern transit authorities. It transforms raw track inspection imagery and drone/locomotive video feeds into actionable maintenance intelligence.

Powered by a fine-tuned **YOLO11** computer vision pipeline, **Spring Boot 4** enterprise backend, **FastAPI** high-throughput inference microservice, and a responsive **Leaflet GIS + Chart.js** analytics dashboard, the platform minimizes human inspection latency from days to milliseconds while eliminating catastrophic derailment risks.

---

## 🏗️ System Architecture

```
                                  TRACKVISION GUARD ARCHITECTURE
                                  
   +--------------------+          +--------------------+          +--------------------+
   |   INSPECTION FEED  |          |   DRONE / ROV FEED |          | LOCOMOTIVE CAMERAS |
   +---------+----------+          +---------+----------+          +---------+----------+
             |                               |                               |
             +-------------------------------+-------------------------------+
                                             |
                                             v
                         +----------------------------------------+
                         |     FRONTEND WEB APPLICATION (SPA)     |
                         |  HTML5 · CSS3 · Bootstrap 5 · Leaflet  |
                         |  Chart.js · GeoJSON All-India Map      |
                         +-------------------+--------------------+
                                             |
                     +-----------------------+-----------------------+
                     | REST / Multipart                              | REST / Auth
                     v                                               v
     +--------------------------------+              +--------------------------------+
     |   AI VISION SERVICE (FastAPI)  |              |   ENTERPRISE BACKEND (Spring)  |
     |   Port: 8000                   |              |   Port: 8080                   |
     |  · YOLO11 Defect Detection     |              |  · Spring Security + JWT Auth  |
     |  · Crack / Spall / Missing     |              |  · Inspection Management       |
     |  · Confidence & Bounding Box   |              |  · Maintenance Work Orders     |
     |  · Model Weights (.pt)         |              |  · PDF / CSV Audit Reports     |
     +---------------+----------------+              +---------------+----------------+
                     |                                               |
                     +-----------------------+-----------------------+
                                             |
                                             v
                             +--------------------------------+
                             |     MYSQL 8+ DATABASE          |
                             |  · users & role-based access   |
                             |  · track inspections & GPS     |
                             |  · detected defects & bounding |
                             |  · scheduled maintenance tasks |
                             +--------------------------------+
```

---

## 📂 Complete Project Repository Structure

```
TrackVision Guard/
├── .env.example                               # Global environment configuration template
├── .gitignore                                 # Git rules (excludes heavy cache, keeps models)
├── README.md                                  # Full system documentation and guide
├── TRACKVISION_GUARD_PPT_Presentation_Master.md # Slide-by-slide master defense presentation
├── yolo11n.pt                                 # YOLO11 nano baseline weights (5.6 MB)
├── yolo11s.pt                                 # YOLO11 small weights (19.3 MB)
│
├── 📂 frontend/                               # Interactive Web UI & Geospatial Dashboard
│   ├── index.html                             # Landing / Portal entry page
│   ├── pages/                                 # Complete Application Views
│   │   ├── login.html                         # User & Inspector Authentication
│   │   ├── register.html                      # New Inspector Registration
│   │   ├── dashboard.html                     # Real-time Executive KPIs & Severity Gauges
│   │   ├── inspection.html                    # Image/Video Upload & Instant AI Defect Scanner
│   │   ├── map.html                           # All-India Interactive 21-Points Leaflet Map
│   │   ├── maintenance.html                   # Work Order Dispatch & Resolution Tracking
│   │   ├── analytics.html                     # Trend Charts, Defect Distribution, Class Breakdown
│   │   └── reports.html                       # PDF / CSV Export & Historical Audit Logs
│   ├── css/
│   │   ├── style.css                          # Core design system & modern glassmorphic theme
│   │   └── components.css                     # Cards, buttons, tables, badges styling
│   ├── js/
│   │   ├── api.js                             # Unified REST client with JWT token management
│   │   ├── auth.js                            # Login, registration, role guards, logout
│   │   ├── dashboard.js                       # Real-time counter animations & KPI feeds
│   │   ├── inspection.js                      # Drag-and-drop file upload & inference rendering
│   │   ├── map.js                             # Leaflet GIS engine, cluster markers, popup cards
│   │   ├── india-tracks.js                    # All-India railway track coordinates & state hubs
│   │   ├── maintenance.js                     # CRUD operations for repair tickets
│   │   └── analytics.js                       # Chart.js initialization & data rendering
│   └── assets/                                # Badges, icons, sample track imagery
│
├── 📂 backend/railguard/                       # Enterprise Spring Boot Application
│   ├── pom.xml                                # Maven build definition and dependencies
│   ├── mvnw / mvnw.cmd                        # Maven wrapper executable
│   └── src/
│       ├── main/
│       │   ├── java/com/railguard/
│       │   │   ├── RailGuardApplication.java  # Spring Boot Main Entrypoint
│       │   │   ├── config/                    # SecurityConfig, CorsConfig, JwtFilter
│       │   │   ├── controller/                # Auth, Inspection, Defect, Maintenance APIs
│       │   │   ├── dto/                       # Request/Response Data Transfer Objects
│       │   │   ├── model/                     # JPA Entities (User, Inspection, Defect, Task)
│       │   │   ├── repository/                # Spring Data JPA interfaces
│       │   │   └── service/                   # Business logic, audit trailing, file storage
│       │   └── resources/
│       │       └── application.properties     # DB credentials, server port, JWT secrets
│       └── test/                              # Unit & Integration test suites
│
├── 📂 ai-service/                             # High-Throughput FastAPI AI Microservice
│   ├── app.py                                 # FastAPI application and inference endpoints
│   ├── requirements.txt                       # Python dependencies (ultralytics, torch, fastapi)
│   ├── Dockerfile                             # Containerization definition
│   └── models/                                # Production Model Checkpoints
│       ├── yolo11_railguard.pt                # TrackVision Guard Fine-tuned Model (21.2 MB)
│       └── yolo11_railguard_v3.pt             # High-accuracy V3 Model (19.1 MB)
│
├── 📂 database/                               # Relational Schema & Seeding
│   └── railguard_schema.sql                   # MySQL 8 table DDL, indexes, default admin seed
│
└── 📂 notebooks/                              # AI Training & Research Notebooks
    ├── 01_data_preprocessing.ipynb           # Track image augmentations and bounding box labels
    ├── 02_yolo11_training.ipynb               # Hyperparameter tuning, mAP@50 optimization
    └── 03_model_evaluation.ipynb              # Confusion matrix, precision-recall curve
```

---

## ⚡ Key Features

1. **AI-Driven Track Defect Detection**:
   - Detects **Surface Cracks**, **Missing Fasteners / Bolts**, **Broken Sleepers**, **Flange Wear**, and **Track Obstacles** in real time.
   - Provides exact pixel coordinates, bounding boxes, class labels, and confidence percentages.

2. **Geospatial GIS Tracking**:
   - Leaflet-based interactive All-India Railway Track visualization with 21 GPS monitoring stations per state.
   - Live color-coded severity markers (🟢 Normal, 🟡 Minor, 🟠 Moderate, 🔴 Critical Hazard).

3. **Maintenance Dispatch & Ticket Lifecycle**:
   - Automatically generates work orders when critical defects are flagged.
   - Track tickets through `PENDING` ➔ `ASSIGNED` ➔ `IN_PROGRESS` ➔ `RESOLVED`.

4. **Executive Dashboard & Real-Time Analytics**:
   - Total tracks inspected, defect frequency rates, high-risk track zones, and resolution SLA metrics.
   - Interactive Chart.js visuals for monthly trends and defect class distributions.

5. **Enterprise Security & Compliance**:
   - Role-Based Access Control (**ADMIN**, **INSPECTOR**, **MAINTENANCE_LEAD**).
   - BCrypt password hashing and stateless JWT token authentication.
   - Comprehensive audit logging and exportable PDF/CSV reports.

---

## 🛠️ Technology Stack

| Layer | Technology | Details |
|---|---|---|
| **Frontend** | HTML5, CSS3, Vanilla JS, Bootstrap 5 | Leaflet.js (GIS), Chart.js (Analytics), FontAwesome |
| **Backend** | Java 17, Spring Boot 4.0.7 | Spring Security, Spring Data JPA, Hibernate, JWT |
| **AI Inference** | Python 3.14, FastAPI, PyTorch | YOLO11 (Ultralytics), OpenCV, NumPy |
| **Database** | MySQL 8.0+ | Relational schema with foreign keys and spatial indexes |
| **Model Weights** | YOLO11 (.pt format) | Fine-tuned weights included directly in repo |

---

## 🚀 Quickstart & Installation

### 1. Database Setup
1. Ensure MySQL is running on port `3306`.
2. Import the schema:
   ```bash
   mysql -u root -p < database/railguard_schema.sql
   ```
   *(Default test login: username `admin`, password `password123`)*

### 2. Run AI Inference Service
```bash
cd ai-service
python -m venv venv
# Windows:
.\venv\Scripts\activate
# Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```
*API docs will be available at `http://localhost:8000/docs`*

### 3. Run Backend (Spring Boot)
```bash
cd backend/railguard
# Windows:
.\mvnw.cmd spring-boot:run
# Linux/macOS:
./mvnw spring-boot:run
```
*Backend runs at `http://localhost:8080`*

### 4. Run Frontend
Open `frontend/index.html` in any browser or launch a static server:
```bash
cd frontend
python -m http.server 5500
```
*Open `http://localhost:5500` in your web browser.*

---

## 📡 API Overview

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | User login & JWT token generation |
| `POST` | `/api/auth/register` | New inspector account creation |
| `POST` | `/api/inspections/upload` | Upload track photo/video for analysis |
| `GET` | `/api/inspections` | Fetch all historical inspections |
| `GET` | `/api/detections` | Fetch detected defects with GPS coordinates |
| `POST` | `/api/maintenance` | Create maintenance ticket |
| `PUT` | `/api/maintenance/{id}` | Update ticket status / mark resolved |
| `POST` | `http://localhost:8000/detect` | Direct FastAPI YOLO11 inference endpoint |

---

## 👥 Contributors & Academic Defense

- **Project Lead & Developer**: Ankadala Triveni
- **System**: TRACKVISION GUARD — Railway Track Defect Detection and Safety Monitoring System
- **Repository**: [https://github.com/Ankadala-Triveni/TRACKVISION-GUARD-FINAL-](https://github.com/Ankadala-Triveni/TRACKVISION-GUARD-FINAL-)
