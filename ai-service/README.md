# PARAKH AI Risk Classification Service

AI-powered scam detection microservice for the **PARAKH** Job & Internship Offer Verification System.

## Overview
This service provides an auxiliary machine-learning risk signal to complement the Java Spring Boot rule-based heuristic engine. It evaluates raw offer text using a transparent TF-IDF feature extraction pipeline and a Logistic Regression classifier.

## Architecture & Design
- **Framework**: FastAPI (Asynchronous Python REST API)
- **Model**: TF-IDF Vectorizer (unigram + bigram, sublinear TF) + Logistic Regression (L2 regularization, `lbfgs`)
- **Persistence**: Scikit-learn Pipeline serialized via `joblib` in `models/model_pipeline.joblib`
- **Output**: Calibrated probability (`riskProbability` ∈ [0.0, 1.0]) and discrete classification (`SUSPICIOUS` vs `LEGITIMATE`).

## Directory Structure
```
ai-service/
├── app/
│   ├── __init__.py
│   ├── main.py          # FastAPI application & lifecycle
│   ├── model.py         # Model loading & inference logic
│   └── schemas.py       # Pydantic validation schemas
├── data/
│   ├── dataset.json     # Prototype development dataset
│   └── README.md        # Dataset documentation & disclaimers
├── models/
│   ├── model_pipeline.joblib
│   └── metrics.json     # Evaluation metrics on held-out test split
├── tests/
│   ├── __init__.py
│   └── test_api.py      # Automated pytest suite
├── train.py             # Pipeline training & evaluation script
├── requirements.txt     # Dependencies
└── README.md
```

## API Specification

### 1. Health Check
- **Endpoint**: `GET /health`
- **Response**:
```json
{
  "status": "UP",
  "modelLoaded": true,
  "modelType": "tfidf-logistic-regression"
}
```

### 2. Predict Risk
- **Endpoint**: `POST /predict`
- **Request Body**:
```json
{
  "offerText": "Direct selection without interview. Pay Rs 2500 registration fee."
}
```
- **Response**:
```json
{
  "riskProbability": 0.9412,
  "classification": "SUSPICIOUS",
  "model": "tfidf-logistic-regression"
}
```

## Running Locally

1. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
2. Train model:
   ```bash
   python train.py
   ```
3. Run test suite:
   ```bash
   pytest tests/
   ```
4. Start API server on port 8000:
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```
