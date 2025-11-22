package com.observability.gateway;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private Tracer tracer;

    @Value("${services.order.url}")
    private String orderServiceUrl;

    @Value("${services.inventory.url}")
    private String inventoryServiceUrl;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.info("Health check requested");
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "gateway-service");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest) {
        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("create-order-flow").startSpan();
        }
        try {
            logger.info("Creating order through gateway: {}", orderRequest);
            
            // Check inventory first
            String itemId = (String) orderRequest.get("itemId");
            if (span != null) {
                span.setAttribute("item.id", itemId);
            }
            
            ResponseEntity<Map> inventoryCheck = restTemplate.getForEntity(
                inventoryServiceUrl + "/api/inventory/" + itemId, 
                Map.class
            );
            
            if (!inventoryCheck.getStatusCode().is2xxSuccessful()) {
                logger.warn("Inventory check failed for item: {}", itemId);
                return ResponseEntity.badRequest().body(Map.of("error", "Inventory check failed"));
            }
            
            // Create order
            ResponseEntity<Map> orderResponse = restTemplate.postForEntity(
                orderServiceUrl + "/api/orders",
                orderRequest,
                Map.class
            );
            
            logger.info("Order created successfully");
            if (span != null) {
                span.setAttribute("order.status", "success");
            }
            return orderResponse;
        } catch (Exception e) {
            logger.error("Error creating order", e);
            if (span != null) {
                span.setAttribute("order.status", "error");
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
    public ResponseEntity<?> getOrders() {
        logger.info("Fetching all orders");
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                orderServiceUrl + "/api/orders",
                Object.class
            );
            return response;
        } catch (Exception e) {
            logger.error("Error fetching orders", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        // Validate input to prevent injection attacks
        if (id == null || id.trim().isEmpty() || id.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid order ID"));
        }
        
        logger.info("Fetching order: {}", id);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                orderServiceUrl + "/api/orders/" + id,
                Map.class
            );
            return response;
        } catch (Exception e) {
            logger.error("Error fetching order: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/inventory/{itemId}")
    public ResponseEntity<?> checkInventory(@PathVariable String itemId) {
        // Validate input to prevent injection attacks
        if (itemId == null || itemId.trim().isEmpty() || itemId.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid item ID"));
        }
        
        logger.info("Checking inventory for item: {}", itemId);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                inventoryServiceUrl + "/api/inventory/" + itemId,
                Map.class
            );
            return response;
        } catch (Exception e) {
            logger.error("Error checking inventory: {}", itemId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }
}
