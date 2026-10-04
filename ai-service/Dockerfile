FROM python:3.11-slim

WORKDIR /app

# Copy dependency specifications first to leverage Docker layer caching
COPY requirements.txt .

# Install dependencies without caching pip wheels
RUN pip install --no-cache-dir -r requirements.txt

# Create dedicated non-root user and group
RUN groupadd -r -g 1001 appgroup && \
    useradd -r -u 1001 -g appgroup -d /app -s /sbin/nologin appuser

# Copy application source code and pre-trained model files with non-root ownership
COPY --chown=appuser:appgroup app/ app/
COPY --chown=appuser:appgroup models/ models/

# Switch to non-root user
USER appuser

# Expose microservice port
EXPOSE 8000

# Start FastAPI application with Uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
