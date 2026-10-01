import pytest
from fastapi.testclient import TestClient

from app.main import app, risk_model
from app.model import RiskModel


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as test_client:
        yield test_client


def test_health_endpoint(client):
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["modelLoaded"] is True
    assert data["modelType"] == "tfidf-logistic-regression"


def test_predict_legitimate_offer(client):
    payload = {
        "offerText": "We are pleased to extend an offer for Associate Software Engineer at Infosys. Following your multi-round technical interviews, your CTC will be 6.5 LPA. Background verification will be initiated on our careers portal."
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "riskProbability" in data
    assert "classification" in data
    assert "model" in data
    assert data["classification"] == "LEGITIMATE"
    assert data["riskProbability"] < 0.50
    assert data["model"] == "tfidf-logistic-regression"


def test_predict_fee_scam_offer(client):
    payload = {
        "offerText": "Direct selection without interview for Amazon! High salary 50,000 per month. Pay Rs 2,500 refundable registration fee and laptop deposit via GPay immediately."
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["classification"] == "SUSPICIOUS"
    assert data["riskProbability"] >= 0.50


def test_predict_phishing_offer(client):
    payload = {
        "offerText": "To confirm your appointment, please reply with the 6-digit OTP code sent to your mobile phone and login to http://candidate-verify.xyz with your net banking password."
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["classification"] == "SUSPICIOUS"
    assert data["riskProbability"] >= 0.50


def test_predict_empty_offer_text(client):
    payload = {"offerText": ""}
    response = client.post("/predict", json=payload)
    assert response.status_code == 422


def test_predict_whitespace_offer_text(client):
    payload = {"offerText": "     \n   "}
    response = client.post("/predict", json=payload)
    assert response.status_code == 422


def test_predict_missing_field(client):
    payload = {}
    response = client.post("/predict", json=payload)
    assert response.status_code == 422
