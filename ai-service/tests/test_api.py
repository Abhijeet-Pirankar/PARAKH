import pytest
from fastapi.testclient import TestClient

import app.main as main_module
from app.main import app
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


def test_health_endpoint_degraded(client):
    """10. Health endpoint: Verify /health returns DEGRADED status when model is not loaded."""
    original_model = main_module.risk_model
    try:
        main_module.risk_model = None
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "DEGRADED"
        assert data["modelLoaded"] is False
        assert data["modelType"] == "tfidf-logistic-regression"
    finally:
        main_module.risk_model = original_model


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


def test_predict_probability_bounds_and_classification_validity(client):
    """3 & 4. Probability bounds and classification validity: Verify bounds and allowed values."""
    payload = {
        "offerText": "We have an open role for a QA Automation Engineer. Send your portfolio to careers@innovative-solutions.com."
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert 0.0 <= data["riskProbability"] <= 1.0
    assert data["classification"] in ("LEGITIMATE", "SUSPICIOUS", "UNCERTAIN")
    assert data["model"] == "tfidf-logistic-regression"


def test_predict_uncertain_classification(client):
    """Verify that an offer around 0.50 probability returns UNCERTAIN classification."""
    payload = {
        "offerText": (
            "Congratulations! You have been shortlisted for a Software Developer Internship at TechNova Solutions.\n"
            "Your interview will be conducted online through Google Meet. There is no registration fee or payment required.\n"
            "Please visit our official website https://www.technovasolutions.com to learn more about the company and internship.\n"
            "Regards, HR Team TechNova Solutions hr@technovasolutions.com"
        )
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert 0.40 <= data["riskProbability"] < 0.60
    assert data["classification"] == "UNCERTAIN"


def test_predict_empty_offer_text(client):
    payload = {"offerText": ""}
    response = client.post("/predict", json=payload)
    assert response.status_code == 422
    data = response.json()
    assert data["error"] == "VALIDATION_ERROR"
    assert "Invalid input provided" in data["message"]


def test_predict_whitespace_offer_text(client):
    payload = {"offerText": "     \n   "}
    response = client.post("/predict", json=payload)
    assert response.status_code == 422
    data = response.json()
    assert data["error"] == "VALIDATION_ERROR"


def test_predict_missing_field(client):
    payload = {}
    response = client.post("/predict", json=payload)
    assert response.status_code == 422
    data = response.json()
    assert data["error"] == "VALIDATION_ERROR"


def test_predict_oversized_offer_text(client):
    """6. Very large input: Verify offerText over 50,000 characters is rejected with HTTP 422."""
    payload = {"offerText": "A" * 50001}
    response = client.post("/predict", json=payload)
    assert response.status_code == 422
    data = response.json()
    assert data["error"] == "VALIDATION_ERROR"
    assert any("50000" in detail for detail in data.get("details", []))


def test_predict_boundary_valid_length(client):
    """6. Boundary check: Verify offerText within limit succeeds."""
    payload = {"offerText": "Legitimate job opening. " * 100}
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "riskProbability" in data


def test_predict_invalid_field_types(client):
    """8. Invalid field types: Verify integer, boolean, list return HTTP 422 without leaking internals."""
    for invalid_val in [12345, True, ["invalid", "list"], None]:
        payload = {"offerText": invalid_val}
        response = client.post("/predict", json=payload)
        assert response.status_code == 422
        data = response.json()
        assert data["error"] == "VALIDATION_ERROR"
        # Verify internal paths or stack traces are not leaked
        raw_text = response.text
        assert "Traceback" not in raw_text
        assert "File \"" not in raw_text


def test_predict_malformed_json_body(client):
    """8. Malformed JSON: Verify invalid JSON returns HTTP 422 without exposing stack traces."""
    response = client.post(
        "/predict",
        content="{invalid json format: missing quotes}",
        headers={"Content-Type": "application/json"}
    )
    assert response.status_code == 422
    data = response.json()
    assert data["error"] == "VALIDATION_ERROR"
    assert "Traceback" not in response.text
    assert "site-packages" not in response.text


def test_predict_model_unavailable_returns_503(client):
    """9. Model unavailable: Verify API returns HTTP 503 when model is uninitialized or not loaded."""
    original_model = main_module.risk_model
    try:
        main_module.risk_model = None
        payload = {"offerText": "Software Engineer offer with standard terms."}
        response = client.post("/predict", json=payload)
        assert response.status_code == 503
        data = response.json()
        assert "AI model is currently not available" in data["detail"]
    finally:
        main_module.risk_model = original_model


def test_predict_robustness_special_characters(client):
    """11. Model bounds / robustness: API handles unusual text with punctuation, numbers, URLs, emails, emojis."""
    complex_text = (
        "💼 URGENT REQUIREMENT for FullStack Dev! Salary: $120,000 / Rs 15,00,000 p.a. (10% bonus). "
        "Apply at https://careers.acme-corp.com/apply?ref=link#123 or email hr-recruit@acme.org. "
        "Selected candidates will receive offer letters within 24 hours!!!!"
    )
    payload = {"offerText": complex_text}
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert 0.0 <= data["riskProbability"] <= 1.0
    assert data["classification"] in ("LEGITIMATE", "SUSPICIOUS", "UNCERTAIN")
    assert data["model"] == "tfidf-logistic-regression"
