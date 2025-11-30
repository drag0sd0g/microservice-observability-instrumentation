package com.observability.order.service;

import com.observability.order.Order;
import com.observability.order.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.observability.commons.util.LogUtils.sanitizeForLog;

/**
 * Service class for order management operations.
 * 
 * <p>This service handles the business logic for order creation
 * and retrieval, including metrics collection and distributed tracing.</p>
 *
 * @since 1.0.0
 */
@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final Counter ordersCreatedCounter;
    private final Tracer tracer;

    /**
     * Constructs a new OrderService with the required dependencies.
     *
     * @param orderRepository the repository for order persistence
     * @param meterRegistry the Micrometer registry for metrics
     * @param tracer the OpenTelemetry tracer for distributed tracing (optional)
     */
    public OrderService(
            final OrderRepository orderRepository,
            final MeterRegistry meterRegistry,
            @Autowired(required = false) final Tracer tracer) {
        this.orderRepository = orderRepository;
        this.ordersCreatedCounter = Counter.builder("orders_created_total")
            .description("Total number of orders created")
            .register(meterRegistry);
        this.tracer = tracer;
    }

    /**
     * Creates a new order with the specified item and quantity.
     *
     * @param itemId the ID of the item to order
     * @param quantity the quantity to order
     * @return the created order with generated ID
     */
    public Order createOrder(final String itemId, final Integer quantity) {
        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("create-order").startSpan();
        }
        try {
            if (span != null) {
                span.setAttribute("order.item_id", itemId);
                span.setAttribute("order.quantity", quantity);
            }

            var order = new Order(itemId, quantity);
            order = orderRepository.save(order);

            ordersCreatedCounter.increment();
            logger.info("Order created: {}", sanitizeForLog(order.getId()));

            return order;
        } catch (Exception e) {
            logger.error("Error creating order", e);
            if (span != null) {
                span.recordException(e);
            }
            throw e;
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    /**
     * Retrieves all orders from the database.
     *
     * @return list of all orders
     */
    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    /**
     * Retrieves a specific order by its ID.
     *
     * @param id the order ID to retrieve
     * @return an Optional containing the order if found, empty otherwise
     */
    public Optional<Order> getOrderById(final String id) {
        logger.info("Fetching order: {}", sanitizeForLog(id));
        return orderRepository.findById(id);
    }
}
