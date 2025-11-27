package com.observability.inventory;

import com.observability.inventory.model.*;
import com.observability.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        logger.info("Health check requested");
        var response = new HealthResponse("UP", "inventory-service");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inventory/{itemId}")
    public ResponseEntity<?> checkInventory(@PathVariable String itemId) {
        // Validate input to prevent injection attacks
        if (itemId == null || itemId.trim().isEmpty() || itemId.length() > 255) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid item ID"));
        }

        try {
            var result = inventoryService.checkInventory(itemId);
            var response = new InventoryResponse()
                .itemId((String) result.get("itemId"))
                .name((String) result.get("name"))
                .quantity((Integer) result.get("quantity"))
                .available((Boolean) result.get("available"));
            return ResponseEntity.ok(response);
        } catch (InterruptedException e) {
            logger.error("Interrupted during latency simulation", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body(new ErrorResponse("Service interrupted"));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Chaos error")) {
                return ResponseEntity.internalServerError().body(new ErrorResponse("Chaos error injected"));
            }
            logger.error("Error checking inventory", e);
            return ResponseEntity.internalServerError().body(new ErrorResponse("Internal server error"));
        }
    }

    @PostMapping("/chaos/latency")
    public ResponseEntity<?> configureChaosLatency(@RequestBody ChaosLatencyRequest config) {
        try {
            var enabled = config.getEnabled();
            var min = config.getMin();
            var max = config.getMax();

            // Validate min
            if (min != null && (min < 0 || min > 10000)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Min latency must be between 0 and 10000"));
            }

            // Validate max
            if (max != null && (max < 0 || max > 10000)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Max latency must be between 0 and 10000"));
            }

            // Update config first to check min/max relationship
            var result = inventoryService.configureChaosLatency(enabled, min, max);

            // Validate min <= max after update
            if (inventoryService.getChaosLatencyMin() > inventoryService.getChaosLatencyMax()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Min latency cannot be greater than max latency"));
            }

            var response = new ChaosLatencyResponse()
                .enabled((Boolean) result.get("enabled"))
                .min((Integer) result.get("min"))
                .max((Integer) result.get("max"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error configuring chaos latency", e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid configuration"));
        }
    }

    @PostMapping("/chaos/errors")
    public ResponseEntity<?> configureChaosErrors(@RequestBody ChaosErrorRequest config) {
        try {
            var enabled = config.getEnabled();
            var rate = config.getRate();

            // Validate rate
            if (rate != null && (rate < 0.0 || rate > 1.0)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Error rate must be between 0.0 and 1.0"));
            }

            var result = inventoryService.configureChaosErrors(enabled, rate);
            var response = new ChaosErrorResponse()
                .enabled((Boolean) result.get("enabled"))
                .rate((Double) result.get("rate"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error configuring chaos errors", e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid configuration"));
        }
    }
}
