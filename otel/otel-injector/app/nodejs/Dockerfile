FROM node:18-alpine

# Create app directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy application code
COPY server.js ./
COPY tracing.js ./

# Environment variables for OpenTelemetry
ENV OTEL_SERVICE_NAME=nodejs-server
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
ENV OTEL_EXPORTER_OTLP_PROTOCOL=grpc
ENV OTEL_RESOURCE_ATTRIBUTES=service.version=1.0.0,deployment.environment=demo,service.instance.id=nodejs-server-1
ENV OTEL_LOG_LEVEL=debug
ENV NODE_OPTIONS="--require ./tracing.js"

# Expose port
EXPOSE 3000

# Start command
CMD ["npm", "start"] 