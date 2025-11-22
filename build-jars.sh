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

# Build Gateway Service
echo "📦 Building Gateway Service..."
cd services/gateway-service
./gradlew clean build -x test --no-daemon
echo "✅ Gateway Service built successfully"
echo ""

# Build Order Service  
echo "📦 Building Order Service..."
cd ../order-service
./gradlew clean build -x test --no-daemon
echo "✅ Order Service built successfully"
echo ""

# Build Inventory Service
echo "📦 Building Inventory Service..."
cd ../inventory-service
./gradlew clean build -x test --no-daemon
echo "✅ Inventory Service built successfully"
echo ""

cd ../..

echo "✨ All services built successfully!"
echo ""
echo "You can now run: docker compose up -d"
