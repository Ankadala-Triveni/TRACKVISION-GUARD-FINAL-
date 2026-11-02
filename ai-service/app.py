"""
RailGuard AI – FastAPI AI Service
Phase 3: Service endpoints including /health and /predict.
"""

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import time
from predict import run_prediction

app = FastAPI(
    title="RailGuard AI Service",
    description="AI-Powered Railway Track Defect Detection Service",
    version="1.0.0-PHASE3",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://127.0.0.1:8080", "*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", tags=["Health"])
async def health_check():
    """Health check endpoint."""
    return {"status": "AI service is running"}


@app.post("/predict", tags=["Prediction"])
async def predict_endpoint(
    inspection_id: int = Form(1),
    file: UploadFile = File(...)
):
    """
    Prediction endpoint called by Spring Boot backend.
    Accepts inspection_id and track image.
    Returns status, model_version, and list of detections.
    If model weights are missing, returns MODEL_NOT_AVAILABLE with empty detections.
    """
    start_time = time.time()
    contents = await file.read()
    
    result = run_prediction(inspection_id, contents)
    elapsed_ms = (time.time() - start_time) * 1000.0
    result["processing_time_ms"] = round(elapsed_ms, 2)
    
    return result


@app.get("/", tags=["Root"])
async def root():
    return {
        "service": "RailGuard AI Service",
        "version": "1.0.0-PHASE3",
        "endpoints": {
            "health": "/health",
            "predict": "/predict",
            "docs": "/docs"
        }
    }
