import json
from pathlib import Path
import pytest
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline

from app.model import RiskModel


@pytest.fixture(scope="module")
def risk_model():
    model = RiskModel()
    assert model.is_loaded, "Model should be successfully loaded from models/model_pipeline.joblib"
    return model


def test_model_loading_and_pipeline_components(risk_model):
    """1. Model loading: Verify saved model loads successfully with expected pipeline steps."""
    assert risk_model.is_loaded is True
    assert isinstance(risk_model.pipeline, Pipeline)
    assert "tfidf" in risk_model.pipeline.named_steps, "Pipeline must contain 'tfidf' step"
    assert "clf" in risk_model.pipeline.named_steps, "Pipeline must contain 'clf' step"

    tfidf_step = risk_model.pipeline.named_steps["tfidf"]
    clf_step = risk_model.pipeline.named_steps["clf"]

    assert isinstance(tfidf_step, TfidfVectorizer), "Feature extractor must be TfidfVectorizer"
    assert isinstance(clf_step, LogisticRegression), "Classifier must be LogisticRegression"


def test_model_missing_file_handling():
    """1. Model loading: Verify RiskModel handles missing model file gracefully without crashing."""
    bogus_path = Path("non_existent_model_file.joblib")
    model = RiskModel(model_path=bogus_path)
    assert model.is_loaded is False
    assert model.pipeline is None

    with pytest.raises(RuntimeError, match="Model pipeline is not loaded"):
        model.predict("Sample text to test unloaded state")


def test_model_prediction_structure_and_bounds(risk_model):
    """3. Probability bounds: riskProbability must always be between 0.0 and 1.0."""
    result = risk_model.predict("Software Engineer position at Google following multi-round interviews.")
    assert "riskProbability" in result
    assert "classification" in result
    assert "model" in result
    assert isinstance(result["riskProbability"], float)
    assert 0.0 <= result["riskProbability"] <= 1.0
    assert result["classification"] in ("LEGITIMATE", "SUSPICIOUS", "UNCERTAIN")
    assert result["model"] == "tfidf-logistic-regression"


def test_model_classification_validity_and_threshold(risk_model):
    """4. Classification validity: classification must be strictly in allowed values."""
    legit_result = risk_model.predict("We are pleased to offer you the position of Systems Analyst at Infosys.")
    assert legit_result["classification"] in ("LEGITIMATE", "SUSPICIOUS", "UNCERTAIN")
    if legit_result["riskProbability"] < 0.40:
        assert legit_result["classification"] == "LEGITIMATE"
    elif legit_result["riskProbability"] >= 0.60:
        assert legit_result["classification"] == "SUSPICIOUS"
    else:
        assert legit_result["classification"] == "UNCERTAIN"

    scam_result = risk_model.predict("Direct selection without interview! Pay Rs 3000 registration fee via GPay now!")
    assert scam_result["classification"] in ("LEGITIMATE", "SUSPICIOUS", "UNCERTAIN")
    if scam_result["riskProbability"] >= 0.60:
        assert scam_result["classification"] == "SUSPICIOUS"
    elif scam_result["riskProbability"] < 0.40:
        assert scam_result["classification"] == "LEGITIMATE"
    else:
        assert scam_result["classification"] == "UNCERTAIN"


def test_model_uncertain_classification_around_half(risk_model):
    """Verify that an offer around 0.50 probability is classified as UNCERTAIN."""
    text_around_half = (
        "Congratulations! You have been shortlisted for a Software Developer Internship at TechNova Solutions.\n"
        "Your interview will be conducted online through Google Meet. There is no registration fee or payment required.\n"
        "Please visit our official website https://www.technovasolutions.com to learn more about the company and internship.\n"
        "Regards, HR Team TechNova Solutions hr@technovasolutions.com"
    )
    result = risk_model.predict(text_around_half)
    assert 0.40 <= result["riskProbability"] < 0.60, f"Expected probability around 0.50, got {result['riskProbability']}"
    assert result["classification"] == "UNCERTAIN"


def test_model_robustness_special_inputs(risk_model):
    """11. Model bounds / robustness: Inference does not crash on unusual but valid text."""
    test_cases = [
        "Congratulations!!!!!! You are HIRED??? Call: +91-9876543210 immediately!!! *** #1 Opportunity ***",
        "Salary: $120,000 / Rs. 15,00,000 per annum + 15% bonus. 2026 Batch.",
        "Apply at https://careers.example.com/jobs/dev-123?ref=linkedin&utm=test",
        "Send resume to hr-recruiting@domain.co.in or support@portal.org",
        "UrGeNt ReQuIrEmEnT: SeNiOr PyThOn EnGiNeEr FoR rEmOtE wOrK",
        "💼 Software Engineer • Remote 🌟 Apply now: test@example.com",
        "Offer letter confirmed.",
    ]
    for text in test_cases:
        result = risk_model.predict(text)
        assert isinstance(result["riskProbability"], float)
        assert 0.0 <= result["riskProbability"] <= 1.0
        assert result["classification"] in ("LEGITIMATE", "SUSPICIOUS", "UNCERTAIN")
        assert result["model"] == "tfidf-logistic-regression"


def test_evaluation_metrics_file_completeness():
    """12. Evaluation metrics: Verify metrics.json exists and contains documented evaluation fields."""
    base_dir = Path(__file__).resolve().parent.parent
    metrics_path = base_dir / "models" / "metrics.json"
    assert metrics_path.exists(), f"metrics.json must exist at {metrics_path}"

    with open(metrics_path, "r", encoding="utf-8") as f:
        metrics = json.load(f)

    # Required metric fields: accuracy, precision, recall, F1
    assert "accuracy" in metrics, "metrics.json must contain 'accuracy'"
    assert "precision" in metrics, "metrics.json must contain 'precision'"
    assert "recall" in metrics, "metrics.json must contain 'recall'"
    assert "f1_score" in metrics, "metrics.json must contain 'f1_score'"

    for field in ["accuracy", "precision", "recall", "f1_score"]:
        val = metrics[field]
        assert isinstance(val, (int, float)), f"Metric '{field}' must be numeric"
        assert 0.0 <= val <= 1.0, f"Metric '{field}' must be between 0.0 and 1.0"

    # Dataset metadata
    assert "dataset_size" in metrics
    assert "train_samples" in metrics
    assert "test_samples" in metrics
    assert metrics["train_samples"] + metrics["test_samples"] == metrics["dataset_size"]
    assert metrics["model_type"] == "tfidf-logistic-regression"

    # Confusion matrix structure
    assert "confusion_matrix" in metrics
    cm = metrics["confusion_matrix"]
    assert "true_negatives" in cm
    assert "false_positives" in cm
    assert "false_negatives" in cm
    assert "true_positives" in cm
