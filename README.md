# Microservices Observability with Grafana Stack

[![CI Build and Test](https://github.com/drag0sd0g/microservice-observability-instrumentation/actions/workflows/ci.yml/badge.svg)](https://github.com/drag0sd0g/microservice-observability-instrumentation/actions/workflows/ci.yml)
[![CodeQL](https://github.com/drag0sd0g/microservice-observability-instrumentation/actions/workflows/codeql.yml/badge.svg)](https://github.com/drag0sd0g/microservice-observability-instrumentation/actions/workflows/codeql.yml)
[![License](https://img.shields.io/github/license/drag0sd0g/microservice-observability-instrumentation)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Gradle Version](https://img.shields.io/badge/Gradle-8.11.1-blue.svg)](https://gradle.org/)

A comprehensive demonstration of modern observability practices using the Grafana stack (Prometheus, Loki, Tempo, Alloy, Grafana) with OpenTelemetry instrumentation in a distributed microservices architecture.

## 🎯 Project Overview

This project showcases a complete observability solution featuring:

- **Distributed Tracing** with Tempo and OpenTelemetry
- **Metrics Collection** with Prometheus and Grafana
- **Logs Aggregation** with Loki
- **Telemetry Pipeline** with Grafana Alloy
- **Alerting** with Prometheus Alertmanager
- **Correlation** between logs, metrics, and traces

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            Client/Browser                               │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                    ┌────────────▼──────────────┐
                    │   Gateway Service :8080   │
                    │   (REST API Gateway)      │
                    └────────┬──────────┬───────┘
                             │          │
              ┌──────────────┘          └─────────────┐
              │                                       │
    ┌─────────▼──────────┐                 ┌──────────▼─────────┐
    │ Order Service      │                 │ Inventory Service  │
    │ :8081              │                 │ :8082              │
    │ (Order Management) │                 │ (Stock Checks)     │
    └─────────┬──────────┘                 └──────────┬─────────┘
              │                                       │
              └────────────────┬──────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │   CockroachDB       │
                    │   :26257            │
                    │   (Distributed DB)  │
                    └─────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                        Observability Stack                              │
│                                                                         │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────────┐      │
│  │ Services │──▶│  Alloy   │──▶│Prometheus│──▶│    Grafana       │      │
│  │  OTLP    │   │  :4317   │   │  :9090   │   │    :3000         │      │
│  └──────────┘   └────┬─────┘   └──────────┘   └──────────────────┘      │
│                      │                                  ▲               │
│                      ├────────────────┐                 │               │
│                      │                │                 │               │
│                 ┌────▼─────┐    ┌────▼─────┐            │               │
│                 │   Loki   │    │  Tempo   │            │               │
│                 │  :3100   │    │  :3200   │────────────┘               │
│                 └──────────┘    └──────────┘                            │
│                                                                         │
│                 ┌──────────────┐                                        │
│                 │ Alertmanager │                                        │
│                 │    :9093     │                                        │
│                 └──────────────┘                                        │
└─────────────────────────────────────────────────────────────────────────┘
```

## 🚀 Services

### Gateway Service (Port 8080)
- REST API gateway routing requests to downstream services
- Orchestrates calls between Order and Inventory services
- Exposes endpoints for order management

### Order Service (Port 8081)
- Manages order creation, retrieval, and listing
- Persists orders in CockroachDB
- Emits custom business metrics (orders_created_total)

### Inventory Service (Port 8082)
- Handles inventory availability checks
- Includes chaos engineering capabilities:
  - Configurable latency injection (500-2000ms)
  - Random error generation (configurable rate)
- Persists inventory data in CockroachDB

## 📊 Observability Stack

### Prometheus (Port 9090)
- Scrapes metrics from all services via `/actuator/prometheus`
- Scrapes metrics from Alloy
- Evaluates alerting rules
- Provides metrics storage and querying

### Loki (Port 3100)
- Aggregates structured JSON logs from services
- Receives logs via Alloy OTLP pipeline
- Provides log querying and filtering

### Tempo (Port 3200)
- Stores and queries distributed traces
- Receives OTLP traces via Alloy
- Generates service graphs and span metrics

### Grafana Alloy (Ports 4317, 4318, 12345)
- Acts as OpenTelemetry collector
- Receives OTLP traces, metrics, and logs
- Processes and routes telemetry to backends
- Scrapes Prometheus metrics from services

### Grafana (Port 3000)
- Unified observability UI
- Pre-provisioned datasources (Prometheus, Loki, Tempo)
- Pre-built dashboards:
  - Service Overview Dashboard
  - RED Metrics Dashboard
- Credentials: admin/admin

### Alertmanager (Port 9093)
- Manages alerts from Prometheus
- Supports routing to multiple channels (configurable)

## 🔧 Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Gradle 8.11.1
- **Database**: CockroachDB (PostgreSQL-compatible)
- **Instrumentation**: OpenTelemetry SDK
- **Logging**: Logback with Logstash JSON encoder
- **Metrics**: Micrometer with Prometheus registry
- **Container**: Docker & Docker Compose

## 📋 Requirements

- Docker 20.10+
- Docker Compose 2.0+
- 8GB RAM minimum (recommended: 16GB)
- Available ports: 3000, 3100, 3200, 4317, 4318, 8080, 8081, 8082, 9090, 9093, 12345, 26257

## 🚀 Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/drag0sd0g/microservice-observability-instrumentation.git
cd microservice-observability-instrumentation
```

### 2. Build the services

```bash
# Quick build (recommended)
./build-jars.sh

# Or build manually
cd services/gateway-service && ./gradlew build && cd ../..
cd services/order-service && ./gradlew build && cd ../..
cd services/inventory-service && ./gradlew build && cd ../..
```

### 3. Start the stack

```bash
docker-compose up -d
```

Wait for all services to be healthy (30-60 seconds):

```bash
docker-compose ps
```

### 4. Access the UIs

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Alloy**: http://localhost:12345
- **Alertmanager**: http://localhost:9093
- **CockroachDB UI**: http://localhost:8080
- **Gateway Service**: http://localhost:8080/api/health

## 📝 Example API Calls

### Health Check
```bash
curl http://localhost:8080/api/health
```

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

### Get Specific Order
```bash
curl http://localhost:8080/api/orders/{order-id}
```

### Check Inventory
```bash
curl http://localhost:8080/api/inventory/ITEM-123
```

## 🎭 Demo Script

The project includes a demo script to generate realistic traffic:

### Run Traffic Simulation
```bash
./demo.sh traffic 300  # Run for 300 seconds (5 minutes)
```

### Manual Operations
```bash
# Create a single order
./demo.sh order ITEM-5 3

# Check inventory
./demo.sh inventory ITEM-5

# Get all orders
./demo.sh orders
```

### Chaos Engineering
```bash
# Enable latency chaos (500-2000ms)
./demo.sh chaos-latency-on

# Disable latency chaos
./demo.sh chaos-latency-off

# Enable error chaos (20% error rate)
./demo.sh chaos-errors-on

# Disable error chaos
./demo.sh chaos-errors-off
```

## 📈 Viewing Observability Data

### Dashboards

1. **Service Overview Dashboard** (`http://localhost:3000/d/service-overview`)
   - Request rate per service
   - Error rate per service
   - Latency percentiles (P50/P90/P99)
   - Live log stream
   - Trace explorer

2. **RED Metrics Dashboard** (`http://localhost:3000/d/red-metrics`)
   - Rate: Requests per second per service
   - Errors: Error percentage per service
   - Duration: P99 latency per service
   - Business metrics: Orders created counter

### Exploring Logs

1. Go to Grafana → Explore
2. Select **Loki** datasource
3. Query examples:
   ```logql
   {service_name="gateway-service"}
   {service_name="order-service"} |= "error"
   {service_name=~".+"} | json | severity="ERROR"
   ```

### Viewing Traces

1. Go to Grafana → Explore
2. Select **Tempo** datasource
3. Use TraceQL queries:
   ```traceql
   {service.name="gateway-service"}
   {name="create-order-flow"}
   ```
4. Click on any trace to view the span waterfall

### Correlating Logs, Metrics, and Traces

1. In a trace view, click "Logs for this span"
2. Grafana automatically queries Loki with the trace_id
3. From a log line with trace_id, click the TraceID link to jump to Tempo
4. Click "Metrics" in trace view to see related Prometheus metrics

## 🔔 Alerting

### Configured Alerts

1. **High Error Rate** (>10% for 2 minutes)
2. **High Latency** (P99 > 500ms for 5 minutes)
3. **Service Down** (no metrics for 1 minute)
4. **High Request Rate** (>100 req/s for 5 minutes)

### View Active Alerts
```bash
# Check Prometheus alerts
curl http://localhost:9090/api/v1/alerts

# Check Alertmanager
curl http://localhost:9093/api/v2/alerts
```

### Configure Notifications

Edit `observability/alertmanager/config.yml` to add:
- Email notifications
- Slack webhooks
- PagerDuty integration
- Custom webhooks

## 🔍 Telemetry Flow

```
┌──────────────┐
│   Services   │
│  (Java App)  │
└──────┬───────┘
       │
       │ OpenTelemetry SDK
       │ - Traces (OTLP/gRPC)
       │ - Metrics (Prometheus)
       │ - Logs (JSON stdout)
       │
       ▼
┌──────────────┐
│    Alloy     │ ◄─── Scrapes Prometheus metrics
│  (Collector) │      from /actuator/prometheus
└──────┬───────┘
       │
       ├────────────┬─────────────┐
       │            │             │
       ▼            ▼             ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│Prometheus│ │   Loki   │ │  Tempo   │
│ (Metrics)│ │  (Logs)  │ │ (Traces) │
└──────────┘ └──────────┘ └──────────┘
       │            │             │
       └────────────┴─────────────┘
                    │
                    ▼
             ┌──────────────┐
             │   Grafana    │
             │  (Unified)   │
             └──────────────┘
```

## 🧪 Testing Scenarios

### Scenario 1: Normal Operations
```bash
./demo.sh traffic 120
```
- Observe normal latencies (P99 < 100ms)
- Check service logs in Grafana
- View distributed traces

### Scenario 2: High Latency
```bash
./demo.sh chaos-latency-on
./demo.sh traffic 60
./demo.sh chaos-latency-off
```
- Watch latency spike in dashboards
- Trigger High Latency alert
- Correlate slow traces with logs

### Scenario 3: Error Injection
```bash
./demo.sh chaos-errors-on
./demo.sh traffic 60
./demo.sh chaos-errors-off
```
- Watch error rate increase
- Trigger High Error Rate alert
- View error spans in traces

### Scenario 4: Service Failure
```bash
docker-compose stop inventory-service
./demo.sh traffic 30
docker-compose start inventory-service
```
- Trigger Service Down alert
- Observe cascading errors in gateway
- Check recovery time

## 🛠️ Development

### Running Tests

```bash
# Gateway Service
cd services/gateway-service
./gradlew test

# Order Service
cd services/order-service
./gradlew test

# Inventory Service
cd services/inventory-service
./gradlew test
```

### Local Development (without Docker)

1. Start CockroachDB locally
2. Start each service with local profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Adding Custom Metrics

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Autowired
private MeterRegistry meterRegistry;

Counter customCounter = Counter.builder("custom_metric_total")
    .description("Description of custom metric")
    .register(meterRegistry);

customCounter.increment();
```

### Adding Custom Spans

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@Autowired
private Tracer tracer;

Span span = tracer.spanBuilder("custom-operation").startSpan();
try {
    span.setAttribute("custom.attribute", "value");
    // Your business logic
} finally {
    span.end();
}
```

## 📊 Key Metrics

### RED Metrics (Rate, Errors, Duration)
- `http_server_requests_seconds_count` - Request count
- `http_server_requests_seconds_sum` - Total duration
- `http_server_requests_seconds_bucket` - Latency histogram

### Business Metrics
- `orders_created_total` - Total orders created

### Custom Attributes in Traces
- `service.name` - Service identifier
- `trace_id` - Distributed trace ID
- `span_id` - Span identifier
- Custom attributes per operation

## 🔐 Security Considerations

⚠️ **This is a demo project. Production deployments should include:**

- Authentication and authorization
- TLS/SSL encryption
- Secrets management (not hardcoded credentials)
- Network policies and segmentation
- Rate limiting
- Input validation and sanitization

## 🐛 Troubleshooting

### Services not starting
```bash
# Check logs
docker-compose logs gateway-service
docker-compose logs order-service
docker-compose logs inventory-service

# Restart services
docker-compose restart
```

### Database connection issues
```bash
# Check CockroachDB health
curl http://localhost:8080/health?ready=1

# Access CockroachDB SQL shell
docker exec -it cockroachdb ./cockroach sql --insecure
```

### Grafana dashboards not loading
```bash
# Restart Grafana
docker-compose restart grafana

# Check datasource connectivity in Grafana UI
```

### No metrics/traces/logs appearing
```bash
# Check Alloy logs
docker-compose logs alloy

# Verify services are sending telemetry
curl http://localhost:8080/actuator/prometheus
```

### Port conflicts
```bash
# Check what's using the ports
netstat -tuln | grep -E '(3000|8080|9090)'

# Stop conflicting services or change ports in docker-compose.yml
```

## 📚 References

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Loki Documentation](https://grafana.com/docs/loki/)
- [Tempo Documentation](https://grafana.com/docs/tempo/)
- [Grafana Alloy Documentation](https://grafana.com/docs/alloy/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## 📄 License

This project is licensed under the terms specified in the LICENSE file.

## 🎓 Learning Outcomes

After working with this project, you will understand:

1. ✅ How to instrument Java applications with OpenTelemetry
2. ✅ Setting up a complete Grafana observability stack
3. ✅ Configuring telemetry pipelines with Grafana Alloy
4. ✅ Creating correlation between logs, metrics, and traces
5. ✅ Building Grafana dashboards for microservices
6. ✅ Setting up alerting rules and notifications
7. ✅ Implementing chaos engineering for resilience testing
8. ✅ Understanding distributed tracing in microservices
9. ✅ Best practices for structured logging
10. ✅ RED metrics pattern for service monitoring

## 🚦 Next Steps

- Add more services to demonstrate service mesh patterns
- Implement exemplars for metrics-to-traces correlation
- Add Kubernetes deployment manifests
- Integrate with CI/CD pipelines
- Add performance testing with load generators
- Implement SLO/SLI monitoring
- Add cost analysis dashboards

---

**Happy Observing! 🔭**
