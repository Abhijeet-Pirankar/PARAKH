import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.model import RiskModel
from app.schemas import HealthResponse, PredictRequest, PredictResponse

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("parakh.ai_service")

# Global model instance
risk_model: RiskModel | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global risk_model
    logger.info("Initializing PARAKH AI Risk Classification Service...")
    risk_model = RiskModel()
    yield
    logger.info("Shutting down PARAKH AI Risk Classification Service...")


app = FastAPI(
    title="PARAKH AI Risk Classification Service",
    description="Baseline TF-IDF + Logistic Regression inference microservice for job and internship scam detection.",
    version="1.0.0",
    lifespan=lifespan
)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = [f"{err['loc'][-1]}: {err['msg']}" for err in exc.errors()]
    logger.warning("Request validation failed: %s", errors)
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "error": "VALIDATION_ERROR",
            "message": "Invalid input provided.",
            "details": errors
        }
    )


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error("Unhandled error during processing: %s", str(exc), exc_info=False)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": "INTERNAL_SERVER_ERROR",
            "message": "An unexpected error occurred while analyzing the offer."
        }
    )


@app.get("/health", response_model=HealthResponse)
def health():
    is_ready = risk_model is not None and risk_model.is_loaded
    return HealthResponse(
        status="UP" if is_ready else "DEGRADED",
        modelLoaded=is_ready,
        modelType="tfidf-logistic-regression"
    )


@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    if risk_model is None or not risk_model.is_loaded:
        logger.error("Predict endpoint called but model is not loaded.")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI model is currently not available."
        )

    # Security: Log only input character count to avoid logging confidential text
    char_count = len(request.offerText)
    logger.info("Processing prediction request with text length=%d characters", char_count)

    try:
        result = risk_model.predict(request.offerText)
        logger.info(
            "Prediction complete: riskProbability=%.4f classification=%s",
            result["riskProbability"],
            result["classification"]
        )
        return PredictResponse(**result)
    except Exception as e:
        logger.error("Error during model inference: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Model prediction failed."
        )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=False)
