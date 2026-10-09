# Multi-stage Docker build for CloudCostOptimizer
# Stage 1: Build JAR
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Distroless / JRE Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create non-root user for security
RUN groupadd -r cloudops && useradd -r -g cloudops cloudops

# Create data directories
RUN mkdir -p /app/data/reports && chown -R cloudops:cloudops /app

COPY --from=builder /build/target/cloud-cost-optimizer-*.jar app.jar
RUN chown cloudops:cloudops app.jar

USER cloudops

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    REPORT_DIR=/app/data/reports

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
