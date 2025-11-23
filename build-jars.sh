#!/bin/bash

# Build all microservices

set -e

echo "🔨 Building all microservices..."
echo "================================"

# Try to find Java 21, fallback to system Java
if [ -d "/usr/lib/jvm/temurin-21-jdk-amd64" ]; then
    export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
elif [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
elif [ -d "/usr/lib/jvm/java-21" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21
else
    # Use system default Java
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
fi

export PATH=$JAVA_HOME/bin:$PATH

echo "Using Java: $(java -version 2>&1 | head -1)"
echo ""

# Build all services using the root Gradle build
echo "📦 Building all services with Gradle..."
./gradlew clean build -x test --no-daemon --console=plain

echo ""
echo "✨ All services built successfully!"
echo ""
echo "Built artifacts:"
echo "  - Gateway Service: services/gateway-service/build/libs/gateway-service-0.0.1-SNAPSHOT.jar"
echo "  - Order Service: services/order-service/build/libs/order-service-0.0.1-SNAPSHOT.jar"
echo "  - Inventory Service: services/inventory-service/build/libs/inventory-service-0.0.1-SNAPSHOT.jar"
echo ""
echo "You can now run: docker compose up -d"
