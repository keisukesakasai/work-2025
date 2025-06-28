# Use Maven base image which includes Java JDK
FROM maven:3.6-jdk-11 AS builder

# Install additional tools
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Verify Java and Maven installation
RUN java -version && javac -version && mvn -version

# Create app directory
WORKDIR /app

# Copy Maven files
COPY pom.xml ./

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Create runtime stage
FROM openjdk:11-jre-slim

# Install additional tools for OpenTelemetry injector
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/otel-java-loadgen-1.0.0.jar /app/app.jar

# Download and install OpenTelemetry Injector simulation
# Create directories for OpenTelemetry injector simulation
RUN mkdir -p /usr/lib/opentelemetry \
    && mkdir -p /etc/opentelemetry/otelinject

# Download OpenTelemetry Java Agent
RUN wget -O /usr/lib/opentelemetry/javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Create a mock libotelinject.so (in real scenario, this comes from the package)
RUN touch /usr/lib/opentelemetry/libotelinject.so

# Create Java configuration file
RUN echo 'JAVA_TOOL_OPTIONS=-javaagent:/usr/lib/opentelemetry/javaagent.jar' > /etc/opentelemetry/otelinject/java.conf \
    && echo 'OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317' >> /etc/opentelemetry/otelinject/java.conf \
    && echo 'OTEL_EXPORTER_OTLP_PROTOCOL=grpc' >> /etc/opentelemetry/otelinject/java.conf \
    && echo 'OTEL_SERVICE_NAME=java-load-generator' >> /etc/opentelemetry/otelinject/java.conf \
    && echo 'OTEL_RESOURCE_ATTRIBUTES=service.version=1.0.0,deployment.environment=demo,service.instance.id=java-loadgen-1' >> /etc/opentelemetry/otelinject/java.conf \
    && echo 'OTEL_PROPAGATION=tracecontext,baggage,b3' >> /etc/opentelemetry/otelinject/java.conf \
    && echo 'OTEL_INSTRUMENTATION_HTTP_CLIENT_ENABLED=true' >> /etc/opentelemetry/otelinject/java.conf \
    && echo 'OTEL_INSTRUMENTATION_OKHTTP_ENABLED=true' >> /etc/opentelemetry/otelinject/java.conf

# Create startup script that sources the configuration
RUN echo '#!/bin/bash' > /start.sh \
    && echo 'set -a' >> /start.sh \
    && echo 'source /etc/opentelemetry/otelinject/java.conf' >> /start.sh \
    && echo 'set +a' >> /start.sh \
    && echo 'cd /app' >> /start.sh \
    && echo 'echo "Starting Java Load Generator with OpenTelemetry..."' >> /start.sh \
    && echo 'echo "JAVA_TOOL_OPTIONS: $JAVA_TOOL_OPTIONS"' >> /start.sh \
    && echo 'echo "OTEL_SERVICE_NAME: $OTEL_SERVICE_NAME"' >> /start.sh \
    && echo 'echo "OTEL_EXPORTER_OTLP_ENDPOINT: $OTEL_EXPORTER_OTLP_ENDPOINT"' >> /start.sh \
    && echo 'exec java -jar app.jar' >> /start.sh \
    && chmod +x /start.sh

# Environment variables for the application
ENV SERVER_URL=http://nodejs-server:3000
ENV REQUEST_INTERVAL=3000
ENV CONCURRENT_REQUESTS=2

# Use the startup script
CMD ["/start.sh"] 