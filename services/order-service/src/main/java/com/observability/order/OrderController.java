package com.observability.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private Tracer tracer;

    private final Counter ordersCreatedCounter;

    public OrderController(MeterRegistry meterRegistry) {
        this.ordersCreatedCounter = Counter.builder("orders_created_total")
            .description("Total number of orders created")
            .register(meterRegistry);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.info("Health check requested");
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "order-service");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest) {
        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("create-order").startSpan();
        }
        try {
            String itemId = (String) orderRequest.get("itemId");
            Integer quantity = (Integer) orderRequest.get("quantity");

            if (itemId == null || quantity == null) {
                logger.warn("Invalid order request: {}", orderRequest);
                return ResponseEntity.badRequest().body(Map.of("error", "itemId and quantity are required"));
            }
            
            // Validate quantity
            if (quantity <= 0 || quantity > 10000) {
                logger.warn("Invalid quantity: {}", quantity);
                return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be between 1 and 10000"));
            }

            if (span != null) {
                span.setAttribute("order.item_id", itemId);
                span.setAttribute("order.quantity", quantity);
            }

            Order order = new Order(itemId, quantity);
            order = orderRepository.save(order);

            ordersCreatedCounter.increment();
            logger.info("Order created: {}", order.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("id", order.getId());
            response.put("itemId", order.getItemId());
            response.put("quantity", order.getQuantity());
            response.put("status", order.getStatus());
            response.put("createdAt", order.getCreatedAt().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating order", e);
            if (span != null) {
                span.recordException(e);
            }
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        logger.info("Fetching all orders");
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        // Validate input to prevent injection attacks
        if (id == null || id.trim().isEmpty() || id.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid order ID"));
        }
        
        logger.info("Fetching order: {}", id);
        return orderRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
