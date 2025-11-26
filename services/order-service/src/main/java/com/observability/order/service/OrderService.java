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

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final Counter ordersCreatedCounter;

    @Autowired(required = false)
    private Tracer tracer;

    public OrderService(OrderRepository orderRepository, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.ordersCreatedCounter = Counter.builder("orders_created_total")
            .description("Total number of orders created")
            .register(meterRegistry);
    }

    public Order createOrder(String itemId, Integer quantity) {
        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("create-order").startSpan();
        }
        try {
            if (span != null) {
                span.setAttribute("order.item_id", itemId);
                span.setAttribute("order.quantity", quantity);
            }

            Order order = new Order(itemId, quantity);
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

    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(String id) {
        logger.info("Fetching order: {}", sanitizeForLog(id));
        return orderRepository.findById(id);
    }

    private String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace("\n", "_").replace("\r", "_").replace("\t", "_");
    }
}
