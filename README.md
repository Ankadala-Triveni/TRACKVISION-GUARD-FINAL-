# RailGuard AI

**AI-Powered Railway Track Defect Detection & Maintenance Intelligence System**

---

## Architecture Overview

```
RailGuardAI/
│
├── frontend/           HTML5 + CSS3 + Vanilla JS + Bootstrap 5 + Chart.js + Leaflet.js
│   ├── pages/          HTML pages (index.html, dashboard.html, etc.)
│   ├── css/            Custom stylesheets
│   ├── js/             JavaScript modules
│   └── assets/         Images, icons, logo
│
├── backend/            Java 17 + Spring Boot 4.0.7 + Spring Security + JPA + Hibernate
│   └── railguard/      Maven project (com.railguard)
│
├── ai-service/         Python 3.14 + FastAPI + YOLO11 + PyTorch + OpenCV
│   └── models/         Trained YOLO model weights (.pt files) – Phase 2+
│
├── dataset/            Railway track images for training/validation – Phase 2+
├── database/           SQL schema files, migration scripts
├── .env.example        Environment variable template
├── .gitignore          Git ignore rules
└── README.md           This file
```

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | HTML5, CSS3, Vanilla JavaScript, Bootstrap 5, Chart.js, Leaflet.js |
| **Backend** | Java 17, Spring Boot 4.0.7, Spring Security, Spring Data JPA, Hibernate |
| **Database** | MySQL 8+ · Database: `railguard_db` |
| **AI Service** | Python 3.14, FastAPI, YOLO11, PyTorch, OpenCV, Ultralytics |
| **Build (Backend)** | Maven Wrapper (`mvnw.cmd`) |
| **Package (AI)** | pip + requirements.txt |

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 17+ |
| Python | 3.10–3.14 |
| MySQL | 8.0+ |
| MySQL Workbench | Any recent version |

> Maven does **not** need to be installed globally — the **Maven Wrapper** (`mvnw.cmd`) is bundled inside `backend/railguard/`.

---

## Setup & Run

### 1. Database

MySQL will auto-create `railguard_db` on first Spring Boot startup.

If you want to create it manually:
```sql
CREATE DATABASE IF NOT EXISTS railguard_db;
```

> ⚠️ Do NOT modify or drop any other databases.

---

### 2. Backend (Spring Boot)

```powershell
cd backend\railguard
.\mvnw.cmd spring-boot:run
```

Spring Boot starts on **http://localhost:8080**

**Verify MySQL connection:**
```
http://localhost:8080/actuator/health
```
Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" }
  }
}
```

**Default credentials (Phase 1 only):**
- Username: `admin`
- Password: `admin123`

---

### 3. Frontend

Open directly in browser:
```
frontend/pages/index.html
```
Or serve with any static server.

---

### 4. AI Service (FastAPI)

```powershell
cd ai-service
pip install fastapi uvicorn
uvicorn app:app --reload --host 0.0.0.0 --port 8000
```

AI Service starts on **http://localhost:8000**

**Verify:**
```
GET http://localhost:8000/health
```
Expected:
```json
{"status": "AI service is running"}
```

**API Docs (auto-generated):**
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

---

## Backend Package Structure

```
com.railguard/
├── controller/     REST controllers (no business logic)
├── service/        Business logic layer
├── repository/     Spring Data JPA repositories
├── entity/         JPA entities (mapped to MySQL tables)
├── dto/            Data Transfer Objects
├── security/       JWT filters, UserDetailsService
├── config/         Spring configuration beans
├── exception/      Custom exceptions, global handler
└── client/         HTTP clients (e.g., AI service REST client)
```

---

## Environment Configuration

Copy `.env.example` to `.env` and update values:

```powershell
copy .env.example .env
```

Edit `.env`:
```env
SPRING_DATASOURCE_PASSWORD=mysql
JWT_SECRET=your_secret_key_here
```

---

## Database Schema (Planned – Phase 2+)

| Table | Description |
|-------|-------------|
| `users` | System users (roles: ADMIN, INSPECTOR, ENGINEER) |
| `inspections` | Track inspection records |
| `detections` | AI-detected defects |
| `risk_scores` | Computed track risk scores |
| `maintenance_tasks` | Maintenance work orders |
| `reports` | Generated inspection reports |
| `notifications` | Alert notifications |
| `audit_logs` | System audit trail |
| `password_reset_otp` | OTP for password reset |

---

## Development Phases

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 1 | ✅ Complete | Folder structure, Spring Boot, MySQL connection, FastAPI skeleton |
| Phase 2 | 🔜 Pending | Authentication (login, registration, OTP, JWT) |
| Phase 3 | 🔜 Pending | YOLO11 AI detection pipeline |
| Phase 4 | 🔜 Pending | Dashboard, reports, maintenance workflow |
| Phase 5 | 🔜 Pending | Real-time notifications, Leaflet maps, analytics |

---

## MySQL Configuration

| Setting | Value |
|---------|-------|
| Host | localhost |
| Port | 3306 |
| Database | `railguard_db` |
| Username | root |
| JDBC URL | `jdbc:mysql://localhost:3306/railguard_db` |
| DDL Auto | `update` (never `create` or `create-drop`) |

---

## Important Rules

- ❌ Do NOT use `spring.jpa.hibernate.ddl-auto=create` or `create-drop`
- ❌ Do NOT modify or drop other MySQL databases
- ❌ Do NOT store passwords in plain text (Phase 2 uses BCrypt)
- ❌ Do NOT commit `.env` files
- ✅ ALL business logic goes in `service/` layer, NOT in controllers
- ✅ MySQL is the single source of truth — no localStorage, no hardcoded users
