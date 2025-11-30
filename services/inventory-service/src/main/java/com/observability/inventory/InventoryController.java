package com.observability.inventory;

import com.observability.inventory.model.*;
import com.observability.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for inventory management operations.
 * 
 * <p>This controller handles inventory checks and chaos engineering
 * configuration for testing and demonstration purposes.</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    /**
     * Constructs a new InventoryController with the required service.
     *
     * @param inventoryService the inventory service for handling business logic
     */
    public InventoryController(final InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Health check endpoint for service status monitoring.
     *
     * @return ResponseEntity with service status information
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        logger.info("Health check requested");
        final var response = new HealthResponse("UP", "inventory-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Checks inventory availability for a specific item.
     *
     * @param itemId the item ID to check inventory for
     * @return ResponseEntity with inventory information or error details
     */
    @GetMapping("/inventory/{itemId}")
    public ResponseEntity<?> checkInventory(@PathVariable String itemId) {
        // Validate input to prevent injection attacks
        if (itemId == null || itemId.trim().isEmpty() || itemId.length() > 255) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid item ID"));
        }

        try {
            final var result = inventoryService.checkInventory(itemId);
            final var response = new InventoryResponse()
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

    /**
     * Configures chaos latency injection settings.
     *
     * @param config the chaos latency configuration
     * @return ResponseEntity with updated configuration or error details
     */
    @PostMapping("/chaos/latency")
    public ResponseEntity<?> configureChaosLatency(@RequestBody ChaosLatencyRequest config) {
        try {
            final var enabled = config.getEnabled();
            final var min = config.getMin();
            final var max = config.getMax();

            // Validate min
            if (min != null && (min < 0 || min > 10000)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Min latency must be between 0 and 10000"));
            }

            // Validate max
            if (max != null && (max < 0 || max > 10000)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Max latency must be between 0 and 10000"));
            }

            // Update config first to check min/max relationship
            final var result = inventoryService.configureChaosLatency(enabled, min, max);

            // Validate min <= max after update
            if (inventoryService.getChaosLatencyMin() > inventoryService.getChaosLatencyMax()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Min latency cannot be greater than max latency"));
            }

            final var response = new ChaosLatencyResponse()
                .enabled((Boolean) result.get("enabled"))
                .min((Integer) result.get("min"))
                .max((Integer) result.get("max"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error configuring chaos latency", e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid configuration"));
        }
    }

    /**
     * Configures chaos error injection settings.
     *
     * @param config the chaos error configuration
     * @return ResponseEntity with updated configuration or error details
     */
    @PostMapping("/chaos/errors")
    public ResponseEntity<?> configureChaosErrors(@RequestBody ChaosErrorRequest config) {
        try {
            final var enabled = config.getEnabled();
            final var rate = config.getRate();

            // Validate rate
            if (rate != null && (rate < 0.0 || rate > 1.0)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Error rate must be between 0.0 and 1.0"));
            }

            final var result = inventoryService.configureChaosErrors(enabled, rate);
            final var response = new ChaosErrorResponse()
                .enabled((Boolean) result.get("enabled"))
                .rate((Double) result.get("rate"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error configuring chaos errors", e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid configuration"));
        }
    }
}
