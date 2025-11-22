package com.observability.inventory;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    private final Random random = new Random();

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
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    @PostMapping("/chaos/latency")
    public ResponseEntity<Map<String, Object>> configureChaosLatency(@RequestBody Map<String, Object> config) {
        logger.info("Configuring chaos latency: {}", config);
        
        if (config.containsKey("enabled")) {
            this.chaosLatencyEnabled = (Boolean) config.get("enabled");
        }
        if (config.containsKey("min")) {
            this.chaosLatencyMin = (Integer) config.get("min");
        }
        if (config.containsKey("max")) {
            this.chaosLatencyMax = (Integer) config.get("max");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", chaosLatencyEnabled);
        response.put("min", chaosLatencyMin);
        response.put("max", chaosLatencyMax);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chaos/errors")
    public ResponseEntity<Map<String, Object>> configureChaosErrors(@RequestBody Map<String, Object> config) {
        logger.info("Configuring chaos errors: {}", config);
        
        if (config.containsKey("enabled")) {
            this.chaosErrorEnabled = (Boolean) config.get("enabled");
        }
        if (config.containsKey("rate")) {
            this.chaosErrorRate = ((Number) config.get("rate")).doubleValue();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", chaosErrorEnabled);
        response.put("rate", chaosErrorRate);
        return ResponseEntity.ok(response);
    }
}
