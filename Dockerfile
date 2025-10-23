# Multi-stage build for optimized image size

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install fontconfig for better PDF text rendering
RUN apk add --no-cache fontconfig ttf-dejavu

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create a non-root user and temp directory
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /app/temp && \
    chown -R appuser:appgroup /app/temp

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/pdf/health || exit 1

# Run the application with temp directory in /app
ENTRYPOINT ["java", "-Djava.io.tmpdir=/app/temp", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
