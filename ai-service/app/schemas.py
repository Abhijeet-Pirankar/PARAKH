from pydantic import BaseModel, Field, field_validator


class PredictRequest(BaseModel):
    offerText: str = Field(
        ...,
        description="The raw or normalized offer text to be evaluated for fraudulent indicators.",
        min_length=1,
        max_length=50000
    )

    @field_validator("offerText")
    @classmethod
    def validate_non_blank(cls, value: str) -> str:
        if not value or not value.strip():
            raise ValueError("offerText must not be empty or whitespace-only.")
        return value.strip()


class PredictResponse(BaseModel):
    riskProbability: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Estimated probability that the offer text exhibits scam or fraudulent patterns."
    )
    classification: str = Field(
        ...,
        description="Categorical risk prediction: 'SUSPICIOUS', 'LEGITIMATE', or 'UNCERTAIN'."
    )
    model: str = Field(
        default="tfidf-logistic-regression",
        description="Identifier of the baseline ML model used for inference."
    )


class HealthResponse(BaseModel):
    status: str
    modelLoaded: bool
    modelType: str
