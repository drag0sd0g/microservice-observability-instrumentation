#!/bin/bash

# Build all microservices

set -e

echo "🔨 Building all microservices..."
echo "================================"

export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 2>/dev/null || export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))

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
