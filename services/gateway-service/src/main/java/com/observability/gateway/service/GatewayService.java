package com.observability.gateway.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static com.observability.commons.util.LogUtils.sanitizeForLog;

/**
 * Service class for gateway operations.
 * 
 * <p>This service orchestrates calls to downstream microservices
 * (Order Service, Inventory Service) and handles distributed tracing.</p>
 *
 * @since 1.0.0
 */
@Service
public class GatewayService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayService.class);

    private final WebClient webClient;
    private final Tracer tracer;
    private final String orderServiceUrl;
    private final String inventoryServiceUrl;

    /**
     * Constructs a new GatewayService with the required dependencies.
     *
     * @param webClientBuilder the WebClient builder for HTTP calls
     * @param orderServiceUrl the URL of the Order Service
     * @param inventoryServiceUrl the URL of the Inventory Service
     * @param tracer the OpenTelemetry tracer for distributed tracing (optional)
     */
    public GatewayService(
            final WebClient.Builder webClientBuilder,
            @Value("${services.order.url}") final String orderServiceUrl,
            @Value("${services.inventory.url}") final String inventoryServiceUrl,
            @Autowired(required = false) final Tracer tracer) {
        this.webClient = webClientBuilder.build();
        this.orderServiceUrl = orderServiceUrl;
        this.inventoryServiceUrl = inventoryServiceUrl;
        this.tracer = tracer;
    }

    /**
     * Creates a new order after verifying inventory availability.
     *
     * @param orderRequest the order request containing itemId and quantity
     * @return the created order response from the Order Service
     * @throws IllegalStateException if inventory check fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrder(final Map<String, Object> orderRequest) {
        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("create-order-flow").startSpan();
        }
        try {
            logger.info("Creating order through gateway");

            // Check inventory first
            final var itemId = (String) orderRequest.get("itemId");
            if (span != null) {
                span.setAttribute("item.id", itemId);
            }

            final var inventoryCheck = webClient.get()
                .uri(inventoryServiceUrl + "/api/inventory/" + itemId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (inventoryCheck == null) {
                logger.warn("Inventory check failed for item: {}", sanitizeForLog(itemId));
                throw new IllegalStateException("Inventory check failed");
            }

            // Create order
            final var orderResponse = (Map<String, Object>) webClient.post()
                .uri(orderServiceUrl + "/api/orders")
                .bodyValue(orderRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            logger.info("Order created successfully");
            if (span != null) {
                span.setAttribute("order.status", "success");
            }
            return orderResponse;
        } catch (Exception e) {
            logger.error("Error creating order", e);
            if (span != null) {
                span.setAttribute("order.status", "error");
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
     * Retrieves all orders from the Order Service.
     *
     * @return list of all orders
     */
    public Object getOrders() {
        logger.info("Fetching all orders");
        return webClient.get()
            .uri(orderServiceUrl + "/api/orders")
            .retrieve()
            .bodyToMono(Object.class)
            .block();
    }

    /**
     * Retrieves a specific order by its ID.
     *
     * @param id the order ID to retrieve
     * @return the order details or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrder(final String id) {
        logger.info("Fetching order: {}", sanitizeForLog(id));
        return webClient.get()
            .uri(orderServiceUrl + "/api/orders/" + id)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    /**
     * Checks inventory availability for a specific item.
     *
     * @param itemId the item ID to check
     * @return inventory information for the item
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkInventory(final String itemId) {
        logger.info("Checking inventory for item: {}", sanitizeForLog(itemId));
        return webClient.get()
            .uri(inventoryServiceUrl + "/api/inventory/" + itemId)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    /**
     * Processes alert webhooks from Alertmanager.
     *
     * @param alertPayload the alert payload from Alertmanager
     */
    public void processAlertWebhook(final Map<String, Object> alertPayload) {
        logger.info("Alert webhook received from Alertmanager");

        final var alerts = alertPayload.get("alerts");
        final var status = (String) alertPayload.get("status");

        if (alerts != null) {
            logger.info("Alert status: {}, number of alerts: {}",
                sanitizeForLog(status),
                alerts instanceof List ? ((List<?>) alerts).size() : 1);
        }

        // Log sanitized payload summary for debugging (avoid logging raw user-controlled data)
        if (logger.isDebugEnabled()) {
            logger.debug("Alert payload received with {} keys", alertPayload.size());
        }
    }
}
