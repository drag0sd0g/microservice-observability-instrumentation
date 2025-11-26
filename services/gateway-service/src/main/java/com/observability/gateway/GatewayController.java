package com.observability.gateway;

import com.observability.gateway.service.GatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.observability.gateway.util.LogUtils.sanitizeForLog;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
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
        try {
            Map<String, Object> orderResponse = gatewayService.createOrder(orderRequest);
            return ResponseEntity.status(201).body(orderResponse);
        } catch (IllegalStateException e) {
            logger.warn("Inventory check failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Inventory check failed"));
        } catch (Exception e) {
            logger.error("Error creating order", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        try {
            Object response = gatewayService.getOrders();
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

        try {
            Map<String, Object> response = gatewayService.getOrder(id);
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

        try {
            Map<String, Object> response = gatewayService.checkInventory(itemId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking inventory: {}", sanitizeForLog(itemId), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/alerts/webhook")
    public ResponseEntity<Map<String, String>> receiveAlertWebhook(@RequestBody Map<String, Object> alertPayload) {
        gatewayService.processAlertWebhook(alertPayload);

        Map<String, String> response = new HashMap<>();
        response.put("status", "received");
        response.put("message", "Alert webhook processed successfully");
        return ResponseEntity.ok(response);
    }
}
