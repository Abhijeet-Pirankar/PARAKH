import logging
from pathlib import Path
import joblib

logger = logging.getLogger("parakh.ai_service.model")


class RiskModel:
    def __init__(self, model_path: Path | None = None):
        if model_path is None:
            base_dir = Path(__file__).resolve().parent.parent
            model_path = base_dir / "models" / "model_pipeline.joblib"
        self.model_path = model_path
        self.pipeline = None
        self.load_model()

    def load_model(self):
        if not self.model_path.exists():
            logger.warning("Model file not found at %s. Inference will not be available until trained.", self.model_path)
            self.pipeline = None
            return

        try:
            self.pipeline = joblib.load(self.model_path)
            logger.info("Successfully loaded model pipeline from %s", self.model_path)
        except Exception as e:
            logger.error("Failed to load model pipeline: %s", str(e))
            self.pipeline = None

    @property
    def is_loaded(self) -> bool:
        return self.pipeline is not None

    def predict(self, text: str) -> dict:
        if not self.is_loaded:
            raise RuntimeError("Model pipeline is not loaded.")

        # Predict probability for SUSPICIOUS class (index 1)
        probabilities = self.pipeline.predict_proba([text])[0]
        suspicious_prob = float(probabilities[1])
        # Round to 4 decimal places
        suspicious_prob = round(suspicious_prob, 4)

        classification = "SUSPICIOUS" if suspicious_prob >= 0.50 else "LEGITIMATE"

        return {
            "riskProbability": suspicious_prob,
            "classification": classification,
            "model": "tfidf-logistic-regression"
        }
