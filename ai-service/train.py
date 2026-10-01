"""
PARAKH AI Service - Model Training & Evaluation Pipeline
Baseline Model: TF-IDF Vectorizer + Logistic Regression Classifier
"""

import json
import os
import joblib
from pathlib import Path
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, classification_report
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline


def train_model():
    base_dir = Path(__file__).resolve().parent
    data_path = base_dir / "data" / "dataset.json"
    models_dir = base_dir / "models"
    models_dir.mkdir(parents=True, exist_ok=True)

    if not data_path.exists():
        raise FileNotFoundError(f"Dataset file not found at {data_path}")

    with open(data_path, "r", encoding="utf-8") as f:
        records = json.load(f)

    texts = [r["text"] for r in records]
    labels = [1 if r["label"] == "SUSPICIOUS" else 0 for r in records]

    # Stratified train/test split (80% train, 20% test)
    x_train, x_test, y_train, y_test = train_test_split(
        texts, labels, test_size=0.20, random_state=42, stratify=labels
    )

    print(f"Dataset loaded: {len(texts)} samples ({sum(labels)} SUSPICIOUS, {len(labels) - sum(labels)} LEGITIMATE)")
    print(f"Training set: {len(x_train)} samples, Test set: {len(x_test)} samples")

    # Pipeline: TF-IDF with unigrams + bigrams and Logistic Regression
    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(
            ngram_range=(1, 2),
            sublinear_tf=True,
            min_df=1,
            max_features=3000,
            lowercase=True
        )),
        ("clf", LogisticRegression(
            C=1.5,
            solver="lbfgs",
            random_state=42,
            max_iter=1000
        ))
    ])

    pipeline.fit(x_train, y_train)

    # Evaluate on held-out test split
    y_pred = pipeline.predict(x_test)
    y_prob = pipeline.predict_proba(x_test)[:, 1]

    acc = float(accuracy_score(y_test, y_pred))
    prec = float(precision_score(y_test, y_pred, zero_division=0))
    rec = float(recall_score(y_test, y_pred, zero_division=0))
    f1 = float(f1_score(y_test, y_pred, zero_division=0))
    cm = confusion_matrix(y_test, y_pred).tolist()

    report = classification_report(y_test, y_pred, target_names=["LEGITIMATE", "SUSPICIOUS"], output_dict=True)

    metrics = {
        "model_type": "tfidf-logistic-regression",
        "dataset_size": len(texts),
        "train_samples": len(x_train),
        "test_samples": len(x_test),
        "accuracy": acc,
        "precision": prec,
        "recall": rec,
        "f1_score": f1,
        "confusion_matrix": {
            "true_negatives": cm[0][0],
            "false_positives": cm[0][1],
            "false_negatives": cm[1][0],
            "true_positives": cm[1][1]
        },
        "classification_report": report
    }

    # Save pipeline artifact
    model_output_path = models_dir / "model_pipeline.joblib"
    joblib.dump(pipeline, model_output_path)
    print(f"Saved trained pipeline to: {model_output_path}")

    # Save metrics JSON
    metrics_output_path = models_dir / "metrics.json"
    with open(metrics_output_path, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)
    print(f"Saved evaluation metrics to: {metrics_output_path}")

    print("\n--- Model Evaluation Summary ---")
    print(f"Accuracy:  {acc:.4f}")
    print(f"Precision: {prec:.4f}")
    print(f"Recall:    {rec:.4f}")
    print(f"F1 Score:  {f1:.4f}")
    print(f"Confusion Matrix (TN, FP, FN, TP): {cm}")

    return metrics


if __name__ == "__main__":
    train_model()
