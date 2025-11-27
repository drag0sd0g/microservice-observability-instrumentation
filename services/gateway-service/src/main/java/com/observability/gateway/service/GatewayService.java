package com.observability.gateway.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static com.observability.gateway.util.LogUtils.sanitizeForLog;

@Service
public class GatewayService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayService.class);

    private final WebClient webClient;
    private final Tracer tracer;
    private final String orderServiceUrl;
    private final String inventoryServiceUrl;

    public GatewayService(
            WebClient.Builder webClientBuilder,
            @Value("${services.order.url}") String orderServiceUrl,
            @Value("${services.inventory.url}") String inventoryServiceUrl,
            @Autowired(required = false) Tracer tracer) {
        this.webClient = webClientBuilder.build();
        this.orderServiceUrl = orderServiceUrl;
        this.inventoryServiceUrl = inventoryServiceUrl;
        this.tracer = tracer;
    }

    public Map<String, Object> createOrder(Map<String, Object> orderRequest) {
        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("create-order-flow").startSpan();
        }
        try {
            logger.info("Creating order through gateway");

            // Check inventory first
            String itemId = (String) orderRequest.get("itemId");
            if (span != null) {
                span.setAttribute("item.id", itemId);
            }

            Map inventoryCheck = webClient.get()
                .uri(inventoryServiceUrl + "/api/inventory/" + itemId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (inventoryCheck == null) {
                logger.warn("Inventory check failed for item: {}", sanitizeForLog(itemId));
                throw new IllegalStateException("Inventory check failed");
            }

            // Create order
            Map orderResponse = webClient.post()
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

    public Object getOrders() {
        logger.info("Fetching all orders");
        return webClient.get()
            .uri(orderServiceUrl + "/api/orders")
            .retrieve()
            .bodyToMono(Object.class)
            .block();
    }

    public Map<String, Object> getOrder(String id) {
        logger.info("Fetching order: {}", sanitizeForLog(id));
        return webClient.get()
            .uri(orderServiceUrl + "/api/orders/" + id)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    public Map<String, Object> checkInventory(String itemId) {
        logger.info("Checking inventory for item: {}", sanitizeForLog(itemId));
        return webClient.get()
            .uri(inventoryServiceUrl + "/api/inventory/" + itemId)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    public void processAlertWebhook(Map<String, Object> alertPayload) {
        logger.info("Alert webhook received from Alertmanager");

        Object alerts = alertPayload.get("alerts");
        String status = (String) alertPayload.get("status");

        if (alerts != null) {
            logger.info("Alert status: {}, number of alerts: {}",
                sanitizeForLog(status),
                alerts instanceof java.util.List ? ((java.util.List<?>) alerts).size() : 1);
        }

        // Log sanitized payload summary for debugging (avoid logging raw user-controlled data)
        if (logger.isDebugEnabled()) {
            logger.debug("Alert payload received with {} keys", alertPayload.size());
        }
    }
}
