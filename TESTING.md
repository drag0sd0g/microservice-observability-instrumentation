# Testing Guide

This guide provides instructions for testing the observability stack.

## Prerequisites

- Docker and Docker Compose installed
- Java 21 installed
- Gradle 8.11.1 (included via wrapper)
- At least 8GB RAM available
- Ports available: 3000, 3100, 3200, 4317, 4318, 8080, 8081, 8082, 9090, 9093, 12345, 26257

## Quick Start Test

### 1. Build All Services

```bash
./build-jars.sh
```

This script will:
- Build Gateway Service
- Build Order Service
- Build Inventory Service
- Skip tests for faster builds

### 2. Start the Stack

```bash
docker compose up -d
```

Wait 30-60 seconds for all services to become healthy:

```bash
docker compose ps
```

All services should show `Up (healthy)` status.

### 3. Verify Services are Running

Check each service health endpoint:

```bash
# Gateway Service
curl http://localhost:8080/api/health

# Order Service (via gateway)
curl http://localhost:8080/api/orders

# Inventory Service (via gateway)
curl http://localhost:8080/api/inventory/ITEM-1
```

### 4. Run Demo Traffic

Generate realistic traffic:

```bash
./demo.sh traffic 60
```

This will run for 60 seconds, creating orders and checking inventory.

### 5. Access Grafana

Open your browser and navigate to:
- **Grafana**: http://localhost:3000 (admin/admin)

Pre-configured dashboards:
- Service Overview Dashboard: http://localhost:3000/d/service-overview
- RED Metrics Dashboard: http://localhost:3000/d/red-metrics

### 6. Test Chaos Engineering

Enable latency chaos:

```bash
./demo.sh chaos-latency-on
./demo.sh traffic 30
./demo.sh chaos-latency-off
```

Enable error chaos:

```bash
./demo.sh chaos-errors-on
./demo.sh traffic 30
./demo.sh chaos-errors-off
```

### 7. View Observability Data

#### Metrics (Prometheus)
http://localhost:9090

Example queries:
```promql
rate(http_server_requests_seconds_count[5m])
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
orders_created_total
```

#### Logs (Grafana → Explore → Loki)
```logql
{service_name="gateway-service"}
{service_name="order-service"} |= "error"
{service_name=~".+"} | json | severity="ERROR"
```

#### Traces (Grafana → Explore → Tempo)
```traceql
{service.name="gateway-service"}
{name="create-order-flow"}
```

### 8. Stop the Stack

```bash
docker compose down
```

**Note**: Data will NOT persist between runs (no volumes are used).

## Manual API Testing

### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"itemId":"ITEM-123","quantity":5}'
```

### Get All Orders

```bash
curl http://localhost:8080/api/orders
```

### Check Inventory

```bash
curl http://localhost:8080/api/inventory/ITEM-123
```

## Troubleshooting

### Services not starting

```bash
# Check logs
docker compose logs gateway-service
docker compose logs order-service
docker compose logs inventory-service

# Restart specific service
docker compose restart gateway-service
```

### Port conflicts

```bash
# Check what's using ports
netstat -tuln | grep -E '(3000|8080|9090)'

# Or change ports in docker-compose.yml
```

### Build failures

```bash
# Clean and rebuild
cd services/gateway-service && ./gradlew clean build
cd ../order-service && ./gradlew clean build
cd ../inventory-service && ./gradlew clean build
```

### Database connection issues

```bash
# Check CockroachDB
docker compose logs cockroachdb

# Access SQL shell
docker exec -it cockroachdb ./cockroach sql --insecure
```

## Testing Checklist

- [ ] All services build successfully
- [ ] Docker Compose starts without errors
- [ ] All health checks pass
- [ ] Can create orders via API
- [ ] Can view orders via API
- [ ] Can check inventory via API
- [ ] Demo script runs successfully
- [ ] Grafana dashboards load
- [ ] Metrics appear in Prometheus
- [ ] Logs appear in Loki
- [ ] Traces appear in Tempo
- [ ] Chaos latency works
- [ ] Chaos errors work
- [ ] Docker Compose stops cleanly
- [ ] No data persists after restart

## Performance Expectations

With the demo script running:

- **Request Rate**: 5-10 requests/second
- **P99 Latency** (normal): < 100ms
- **P99 Latency** (with chaos): 500-2000ms
- **Error Rate** (normal): < 1%
- **Error Rate** (with chaos errors): ~20%

## CI/CD Testing

The project includes GitHub Actions workflows:

- **CI Build and Test**: Runs on every push
- **CodeQL**: Security analysis
- **Release**: Automatic tagging on main branch

Check workflow status:
https://github.com/drag0sd0g/microservice-observability-instrumentation/actions
