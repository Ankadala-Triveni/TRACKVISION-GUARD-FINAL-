# ==============================================================================
# TRACKVISION GUARD - Master Project Launcher
# Railway Track Defect Detection and Safety Monitoring System
# ==============================================================================

Write-Host ""
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "   TRACKVISION GUARD - COMPLETE SYSTEM LAUNCHER                  " -ForegroundColor Yellow
Write-Host "   AI-Powered Railway Track Defect Detection and Monitoring      " -ForegroundColor White
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host ""

$PROJECT_ROOT = "C:\Users\Ankadala Triveni\OneDrive\Documents\TrackVision Guard"
if (-not (Test-Path -Path $PROJECT_ROOT)) {
    $PROJECT_ROOT = $PSScriptRoot
}
Set-Location -Path $PROJECT_ROOT

# 1. Check Python Virtual Environment
$PYTHON_EXE = Join-Path $PROJECT_ROOT ".venv\Scripts\python.exe"
if (-not (Test-Path -Path $PYTHON_EXE)) {
    Write-Host "[!] Python venv not found at .venv. Looking for system python..." -ForegroundColor Yellow
    $PYTHON_EXE = "python"
} else {
    Write-Host "[OK] Python Virtual Environment located: $PYTHON_EXE" -ForegroundColor Green
}

# 2. Check Database Service (Port 3306)
Write-Host "[*] Checking MySQL Service..." -ForegroundColor Gray
$mysqlCheck = Get-NetTCPConnection -LocalPort 3306 -ErrorAction SilentlyContinue
if ($mysqlCheck) {
    Write-Host "[OK] MySQL is running on port 3306." -ForegroundColor Green
} else {
    Write-Host "[!] MySQL is not detected on port 3306. Attempting to start MySQL..." -ForegroundColor Yellow
    Start-Service MySQL* -ErrorAction SilentlyContinue
}

# 3. Start Frontend Web Server (Port 5500)
$port5500 = Get-NetTCPConnection -LocalPort 5500 -ErrorAction SilentlyContinue
if ($port5500) {
    Write-Host "[OK] Frontend server is already active on port 5500." -ForegroundColor Green
} else {
    Write-Host "[+] Starting Frontend HTTP Server on port 5500..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PROJECT_ROOT'; & '$PYTHON_EXE' -m http.server 5500 --directory frontend"
    Start-Sleep -Seconds 1
}

# 4. Start AI Vision Microservice (Port 8000)
$port8000 = Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue
if ($port8000) {
    Write-Host "[OK] FastAPI YOLO11 AI Service is already active on port 8000." -ForegroundColor Green
} else {
    Write-Host "[+] Starting FastAPI AI Service on port 8000..." -ForegroundColor Cyan
    $aiDir = Join-Path $PROJECT_ROOT "ai-service"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$aiDir'; & '$PYTHON_EXE' -m uvicorn app:app --host 0.0.0.0 --port 8000 --reload"
    Start-Sleep -Seconds 2
}

# 5. Start Spring Boot Backend (Port 8080)
$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port8080) {
    Write-Host "[OK] Spring Boot Backend is already active on port 8080." -ForegroundColor Green
} else {
    Write-Host "[+] Starting Spring Boot Backend on port 8080..." -ForegroundColor Cyan
    $backendDir = Join-Path $PROJECT_ROOT "backend\railguard"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir'; .\mvnw.cmd spring-boot:run"
    Start-Sleep -Seconds 3
}

# 6. Launch Application in Browser
Write-Host ""
Write-Host "==================================================================" -ForegroundColor Green
Write-Host "   ALL TRACKVISION GUARD SERVICES ARE ONLINE!                     " -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Green
Write-Host "   Frontend UI:      http://localhost:5500/pages/login.html       " -ForegroundColor Yellow
Write-Host "   FastAPI Swagger:  http://localhost:8000/docs                   " -ForegroundColor Cyan
Write-Host "   Spring Boot API:  http://localhost:8080                        " -ForegroundColor Magenta
Write-Host "   Admin Login:      admin / password123                          " -ForegroundColor White
Write-Host "==================================================================" -ForegroundColor Green
Write-Host ""

Start-Process "http://localhost:5500/pages/login.html"
