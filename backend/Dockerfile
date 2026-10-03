# ==========================================
# Stage 1: Build the Spring Boot Application
# ==========================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# Copy Maven Wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Normalize line endings and ensure executable permissions for the wrapper script
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies in batch mode
RUN ./mvnw dependency:go-offline -B

# Copy application source tree
COPY src/ src/

# Build and package executable Spring Boot JAR (skipping tests during image assembly)
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Stage 2: Minimal Production Runtime Image
# ==========================================
FROM eclipse-temurin:17-jre AS runner

WORKDIR /app

# Create a dedicated non-root user and group (principle of least privilege)
RUN groupadd -r -g 1001 appgroup && \
    useradd -r -u 1001 -g appgroup -d /app -s /sbin/nologin appuser

# Copy the packaged Spring Boot JAR from builder stage with non-root ownership
COPY --from=builder --chown=appuser:appgroup /build/target/recruitshield-backend-*.jar app.jar

# Switch to the non-root user
USER appuser:appgroup

# Expose standard application port
EXPOSE 8080

# Run the packaged executable JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
