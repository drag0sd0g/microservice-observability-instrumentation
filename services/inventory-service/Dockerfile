# Inventory Service Dockerfile
# Uses Eclipse Temurin JRE 21 on Alpine Linux for a minimal image size

FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY build/libs/*.jar app.jar

# Expose the service port
EXPOSE 8082

# JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check to verify service is running
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
