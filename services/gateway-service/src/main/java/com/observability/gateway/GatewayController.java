package com.observability.gateway;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final WebClient webClient;

    @Autowired(required = false)
    private Tracer tracer;

    @Value("${services.order.url}")
    private String orderServiceUrl;

    @Value("${services.inventory.url}")
    private String inventoryServiceUrl;

    public GatewayController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    // Sanitize user input for logging to prevent log injection
    private String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace("\n", "_").replace("\r", "_").replace("\t", "_");
    }

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
            logger.info("Creating order through gateway");
            
            // Check inventory first
            String itemId = (String) orderRequest.get("itemId");
            if (span != null) {
                span.setAttribute("item.id", itemId);
            }
            
            Map inventoryCheck = webClient.get()
                .uri(inventoryServiceUrl + "/api/inventory/" + itemId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (inventoryCheck == null) {
                logger.warn("Inventory check failed for item: {}", sanitizeForLog(itemId));
                return ResponseEntity.badRequest().body(Map.of("error", "Inventory check failed"));
            }
            
            // Create order
            Map orderResponse = webClient.post()
                .uri(orderServiceUrl + "/api/orders")
                .bodyValue(orderRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            logger.info("Order created successfully");
            if (span != null) {
                span.setAttribute("order.status", "success");
            }
            return ResponseEntity.status(201).body(orderResponse);
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
            Object response = webClient.get()
                .uri(orderServiceUrl + "/api/orders")
                .retrieve()
                .bodyToMono(Object.class)
                .block();
            return ResponseEntity.ok(response);
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
        
        logger.info("Fetching order: {}", sanitizeForLog(id));
        try {
            Map response = webClient.get()
                .uri(orderServiceUrl + "/api/orders/" + id)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching order: {}", sanitizeForLog(id), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/inventory/{itemId}")
    public ResponseEntity<?> checkInventory(@PathVariable String itemId) {
        // Validate input to prevent injection attacks
        if (itemId == null || itemId.trim().isEmpty() || itemId.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid item ID"));
        }
        
        logger.info("Checking inventory for item: {}", sanitizeForLog(itemId));
        try {
            Map response = webClient.get()
                .uri(inventoryServiceUrl + "/api/inventory/" + itemId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking inventory: {}", sanitizeForLog(itemId), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }
}
