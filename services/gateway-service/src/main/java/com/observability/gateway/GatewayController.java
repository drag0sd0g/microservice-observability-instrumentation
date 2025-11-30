package com.observability.gateway;

import com.observability.gateway.service.GatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.observability.commons.util.LogUtils.sanitizeForLog;

/**
 * REST controller for the API Gateway service.
 * 
 * <p>This controller serves as the entry point for all client requests,
 * routing them to the appropriate downstream services (Order Service,
 * Inventory Service).</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final GatewayService gatewayService;

    /**
     * Constructs a new GatewayController with the required service.
     *
     * @param gatewayService the gateway service for handling business logic
     */
    public GatewayController(final GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    /**
     * Health check endpoint for service status monitoring.
     *
     * @return ResponseEntity with service status information
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.info("Health check requested");
        final var response = new HashMap<String, String>();
        response.put("status", "UP");
        response.put("service", "gateway-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new order after checking inventory availability.
     *
     * @param orderRequest the order request containing itemId and quantity
     * @return ResponseEntity with the created order or error details
     */
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest) {
        try {
            final var orderResponse = gatewayService.createOrder(orderRequest);
            return ResponseEntity.status(201).body(orderResponse);
        } catch (IllegalStateException e) {
            logger.warn("Inventory check failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Inventory check failed"));
        } catch (Exception e) {
            logger.error("Error creating order", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Retrieves all orders from the Order Service.
     *
     * @return ResponseEntity with the list of orders or error details
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        try {
            final var response = gatewayService.getOrders();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching orders", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Retrieves a specific order by its ID.
     *
     * @param id the order ID to retrieve
     * @return ResponseEntity with the order or error details
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        // Validate input to prevent injection attacks
        if (id == null || id.trim().isEmpty() || id.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid order ID"));
        }

        try {
            final var response = gatewayService.getOrder(id);
            if (response == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching order: {}", sanitizeForLog(id), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
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
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid item ID"));
        }

        try {
            final var response = gatewayService.checkInventory(itemId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking inventory: {}", sanitizeForLog(itemId), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Receives and processes alert webhooks from Alertmanager.
     *
     * @param alertPayload the alert payload from Alertmanager
     * @return ResponseEntity confirming webhook receipt
     */
    @PostMapping("/alerts/webhook")
    public ResponseEntity<Map<String, String>> receiveAlertWebhook(@RequestBody Map<String, Object> alertPayload) {
        gatewayService.processAlertWebhook(alertPayload);

        final var response = new HashMap<String, String>();
        response.put("status", "received");
        response.put("message", "Alert webhook processed successfully");
        return ResponseEntity.ok(response);
    }
}
