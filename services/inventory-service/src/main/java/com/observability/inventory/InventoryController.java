package com.observability.inventory;

import com.observability.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

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

        try {
            Map<String, Object> response = inventoryService.checkInventory(itemId);
            return ResponseEntity.ok(response);
        } catch (InterruptedException e) {
            logger.error("Interrupted during latency simulation", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body(Map.of("error", "Service interrupted"));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Chaos error")) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Chaos error injected"));
            }
            logger.error("Error checking inventory", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/chaos/latency")
    public ResponseEntity<Map<String, Object>> configureChaosLatency(@RequestBody Map<String, Object> config) {
        try {
            Boolean enabled = config.containsKey("enabled") ? (Boolean) config.get("enabled") : null;
            Integer min = config.containsKey("min") ? ((Number) config.get("min")).intValue() : null;
            Integer max = config.containsKey("max") ? ((Number) config.get("max")).intValue() : null;

            // Validate min
            if (min != null && (min < 0 || min > 10000)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Min latency must be between 0 and 10000"));
            }

            // Validate max
            if (max != null && (max < 0 || max > 10000)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Max latency must be between 0 and 10000"));
            }

            // Update config first to check min/max relationship
            Map<String, Object> response = inventoryService.configureChaosLatency(enabled, min, max);

            // Validate min <= max after update
            if (inventoryService.getChaosLatencyMin() > inventoryService.getChaosLatencyMax()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Min latency cannot be greater than max latency"));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error configuring chaos latency", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid configuration"));
        }
    }

    @PostMapping("/chaos/errors")
    public ResponseEntity<Map<String, Object>> configureChaosErrors(@RequestBody Map<String, Object> config) {
        try {
            Boolean enabled = config.containsKey("enabled") ? (Boolean) config.get("enabled") : null;
            Double rate = config.containsKey("rate") ? ((Number) config.get("rate")).doubleValue() : null;

            // Validate rate
            if (rate != null && (rate < 0.0 || rate > 1.0)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Error rate must be between 0.0 and 1.0"));
            }

            Map<String, Object> response = inventoryService.configureChaosErrors(enabled, rate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error configuring chaos errors", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid configuration"));
        }
    }
}
