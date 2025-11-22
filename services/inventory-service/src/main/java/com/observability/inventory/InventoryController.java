package com.observability.inventory;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    private final SecureRandom random = new SecureRandom();

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired(required = false)
    private Tracer tracer;

    @Value("${chaos.latency.enabled:false}")
    private boolean chaosLatencyEnabled;

    @Value("${chaos.latency.min:100}")
    private int chaosLatencyMin;

    @Value("${chaos.latency.max:2000}")
    private int chaosLatencyMax;

    @Value("${chaos.error.enabled:false}")
    private boolean chaosErrorEnabled;

    @Value("${chaos.error.rate:0.1}")
    private double chaosErrorRate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.info("Health check requested");
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "inventory-service");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inventory/{itemId}")
    public ResponseEntity<?> checkInventory(@PathVariable String itemId) {
        // Validate input to prevent injection attacks
        if (itemId == null || itemId.trim().isEmpty() || itemId.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid item ID"));
        }
        
        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("check-inventory").startSpan();
        }
        try {
            if (span != null) {
                span.setAttribute("inventory.item_id", itemId);
            }
            
            // Chaos engineering: random latency
            if (chaosLatencyEnabled) {
                int delay = random.nextInt(chaosLatencyMax - chaosLatencyMin) + chaosLatencyMin;
                if (span != null) {
                    span.setAttribute("chaos.latency_ms", delay);
                }
                logger.warn("Chaos latency injected: {}ms", delay);
                Thread.sleep(delay);
            }

            // Chaos engineering: random errors
            if (chaosErrorEnabled && random.nextDouble() < chaosErrorRate) {
                logger.error("Chaos error injected for item: {}", itemId);
                if (span != null) {
                    span.setAttribute("chaos.error", true);
                }
                return ResponseEntity.internalServerError().body(Map.of("error", "Chaos error injected"));
            }

            logger.info("Checking inventory for item: {}", itemId);
            
            InventoryItem item = inventoryRepository.findById(itemId).orElse(null);
            
            if (item == null) {
                // Return default inventory for demo purposes
                Map<String, Object> response = new HashMap<>();
                response.put("itemId", itemId);
                response.put("name", "Item " + itemId);
                response.put("quantity", 100);
                response.put("available", true);
                return ResponseEntity.ok(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("itemId", item.getItemId());
            response.put("name", item.getName());
            response.put("quantity", item.getQuantity());
            response.put("available", item.getQuantity() > 0);

            return ResponseEntity.ok(response);
        } catch (InterruptedException e) {
            logger.error("Interrupted during latency simulation", e);
            if (span != null) {
                span.recordException(e);
            }
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body(Map.of("error", "Service interrupted"));
        } catch (Exception e) {
            logger.error("Error checking inventory", e);
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

    @PostMapping("/chaos/latency")
    public ResponseEntity<Map<String, Object>> configureChaosLatency(@RequestBody Map<String, Object> config) {
        logger.info("Configuring chaos latency: {}", config);
        
        try {
            if (config.containsKey("enabled")) {
                this.chaosLatencyEnabled = (Boolean) config.get("enabled");
            }
            if (config.containsKey("min")) {
                int min = ((Number) config.get("min")).intValue();
                if (min < 0 || min > 10000) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Min latency must be between 0 and 10000"));
                }
                this.chaosLatencyMin = min;
            }
            if (config.containsKey("max")) {
                int max = ((Number) config.get("max")).intValue();
                if (max < 0 || max > 10000) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Max latency must be between 0 and 10000"));
                }
                this.chaosLatencyMax = max;
            }
            
            if (chaosLatencyMin > chaosLatencyMax) {
                return ResponseEntity.badRequest().body(Map.of("error", "Min latency cannot be greater than max latency"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("enabled", chaosLatencyEnabled);
            response.put("min", chaosLatencyMin);
            response.put("max", chaosLatencyMax);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error configuring chaos latency", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid configuration"));
        }
    }

    @PostMapping("/chaos/errors")
    public ResponseEntity<Map<String, Object>> configureChaosErrors(@RequestBody Map<String, Object> config) {
        logger.info("Configuring chaos errors: {}", config);
        
        try {
            if (config.containsKey("enabled")) {
                this.chaosErrorEnabled = (Boolean) config.get("enabled");
            }
            if (config.containsKey("rate")) {
                double rate = ((Number) config.get("rate")).doubleValue();
                if (rate < 0.0 || rate > 1.0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Error rate must be between 0.0 and 1.0"));
                }
                this.chaosErrorRate = rate;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("enabled", chaosErrorEnabled);
            response.put("rate", chaosErrorRate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error configuring chaos errors", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid configuration"));
        }
    }
}
