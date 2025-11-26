package com.observability.order;

import com.observability.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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
        response.put("service", "order-service");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest) {
        try {
            String itemId = (String) orderRequest.get("itemId");
            Integer quantity = (Integer) orderRequest.get("quantity");

            if (itemId == null || quantity == null) {
                logger.warn("Invalid order request received");
                return ResponseEntity.badRequest().body(Map.of("error", "itemId and quantity are required"));
            }

            // Validate quantity
            if (quantity <= 0 || quantity > 10000) {
                logger.warn("Invalid quantity: {}", quantity);
                return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be between 1 and 10000"));
            }

            Order order = orderService.createOrder(itemId, quantity);

            Map<String, Object> response = new HashMap<>();
            response.put("id", order.getId());
            response.put("itemId", order.getItemId());
            response.put("quantity", order.getQuantity());
            response.put("status", order.getStatus());
            response.put("createdAt", order.getCreatedAt().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating order", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        // Validate input to prevent injection attacks
        if (id == null || id.trim().isEmpty() || id.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid order ID"));
        }

        return orderService.getOrderById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
