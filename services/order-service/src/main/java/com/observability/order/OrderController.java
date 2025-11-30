package com.observability.order;

import com.observability.order.model.CreateOrderRequest;
import com.observability.order.model.ErrorResponse;
import com.observability.order.model.HealthResponse;
import com.observability.order.model.OrderResponse;
import com.observability.order.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;

/**
 * REST controller for order management operations.
 * 
 * <p>This controller handles CRUD operations for orders, including
 * order creation, retrieval, and listing.</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    /**
     * Constructs a new OrderController with the required service.
     *
     * @param orderService the order service for handling business logic
     */
    public OrderController(final OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Health check endpoint for service status monitoring.
     *
     * @return ResponseEntity with service status information
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        logger.info("Health check requested");
        final var response = new HealthResponse("UP", "order-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new order.
     *
     * @param orderRequest the order request containing itemId and quantity
     * @return ResponseEntity with the created order or error details
     */
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest orderRequest) {
        try {
            final var itemId = orderRequest.getItemId();
            final var quantity = orderRequest.getQuantity();

            if (itemId == null || quantity == null) {
                logger.warn("Invalid order request received");
                return ResponseEntity.badRequest().body(new ErrorResponse("itemId and quantity are required"));
            }

            // Validate quantity
            if (quantity <= 0 || quantity > 10000) {
                logger.warn("Invalid quantity: {}", quantity);
                return ResponseEntity.badRequest().body(new ErrorResponse("Quantity must be between 1 and 10000"));
            }

            final var order = orderService.createOrder(itemId, quantity);

            final var response = new OrderResponse()
                .id(order.getId())
                .itemId(order.getItemId())
                .quantity(order.getQuantity())
                .status(OrderResponse.StatusEnum.fromValue(order.getStatus()))
                .createdAt(order.getCreatedAt().atOffset(ZoneOffset.UTC));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating order", e);
            return ResponseEntity.internalServerError().body(new ErrorResponse("Internal server error"));
        }
    }

    /**
     * Retrieves all orders.
     *
     * @return ResponseEntity with the list of all orders
     */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        final var orders = orderService.getAllOrders();
        final var response = orders.stream()
            .map(order -> new OrderResponse()
                .id(order.getId())
                .itemId(order.getItemId())
                .quantity(order.getQuantity())
                .status(OrderResponse.StatusEnum.fromValue(order.getStatus()))
                .createdAt(order.getCreatedAt().atOffset(ZoneOffset.UTC)))
            .toList();
        return ResponseEntity.ok(response);
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
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid order ID"));
        }

        return orderService.getOrderById(id)
            .map(order -> {
                final var response = new OrderResponse()
                    .id(order.getId())
                    .itemId(order.getItemId())
                    .quantity(order.getQuantity())
                    .status(OrderResponse.StatusEnum.fromValue(order.getStatus()))
                    .createdAt(order.getCreatedAt().atOffset(ZoneOffset.UTC));
                return ResponseEntity.ok((Object) response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
