package com.observability.inventory.service;

import com.observability.inventory.InventoryItem;
import com.observability.inventory.InventoryRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.observability.commons.util.LogUtils.sanitizeForLog;

/**
 * Service class for inventory management operations.
 * 
 * <p>This service handles inventory checks and provides chaos engineering
 * capabilities for testing system resilience under adverse conditions.</p>
 *
 * @since 1.0.0
 */
@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    private final SecureRandom random = new SecureRandom();

    private final InventoryRepository inventoryRepository;
    private final Tracer tracer;

    private boolean chaosLatencyEnabled;
    private int chaosLatencyMin;
    private int chaosLatencyMax;
    private boolean chaosErrorEnabled;
    private double chaosErrorRate;

    /**
     * Constructs a new InventoryService with the required dependencies.
     *
     * @param inventoryRepository the repository for inventory persistence
     * @param tracer the OpenTelemetry tracer for distributed tracing (optional)
     * @param chaosLatencyEnabled whether chaos latency is enabled
     * @param chaosLatencyMin minimum latency in milliseconds for chaos injection
     * @param chaosLatencyMax maximum latency in milliseconds for chaos injection
     * @param chaosErrorEnabled whether chaos errors are enabled
     * @param chaosErrorRate the rate of chaos errors (0.0 to 1.0)
     */
    public InventoryService(
            final InventoryRepository inventoryRepository,
            @Autowired(required = false) final Tracer tracer,
            @Value("${chaos.latency.enabled:false}") final boolean chaosLatencyEnabled,
            @Value("${chaos.latency.min:100}") final int chaosLatencyMin,
            @Value("${chaos.latency.max:2000}") final int chaosLatencyMax,
            @Value("${chaos.error.enabled:false}") final boolean chaosErrorEnabled,
            @Value("${chaos.error.rate:0.1}") final double chaosErrorRate) {
        this.inventoryRepository = inventoryRepository;
        this.tracer = tracer;
        this.chaosLatencyEnabled = chaosLatencyEnabled;
        this.chaosLatencyMin = chaosLatencyMin;
        this.chaosLatencyMax = chaosLatencyMax;
        this.chaosErrorEnabled = chaosErrorEnabled;
        this.chaosErrorRate = chaosErrorRate;
    }

    /**
     * Checks inventory availability for a specific item.
     *
     * @param itemId the item ID to check
     * @return map containing inventory information
     * @throws InterruptedException if the thread is interrupted during latency injection
     * @throws RuntimeException if chaos error is injected
     */
    public Map<String, Object> checkInventory(final String itemId) throws InterruptedException {
        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("check-inventory").startSpan();
        }
        try {
            if (span != null) {
                span.setAttribute("inventory.item_id", itemId);
            }

            // Chaos engineering: random latency
            if (chaosLatencyEnabled) {
                final var delay = random.nextInt(chaosLatencyMax - chaosLatencyMin) + chaosLatencyMin;
                if (span != null) {
                    span.setAttribute("chaos.latency_ms", delay);
                }
                logger.warn("Chaos latency injected: {}ms", delay);
                Thread.sleep(delay);
            }

            // Chaos engineering: random errors
            if (chaosErrorEnabled && random.nextDouble() < chaosErrorRate) {
                logger.error("Chaos error injected for item: {}", sanitizeForLog(itemId));
                if (span != null) {
                    span.setAttribute("chaos.error", true);
                }
                throw new RuntimeException("Chaos error injected");
            }

            logger.info("Checking inventory for item: {}", sanitizeForLog(itemId));

            final var itemOpt = inventoryRepository.findById(itemId);

            return buildResponse(itemId, itemOpt);
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    private static HashMap<String, Object> buildResponse(final String itemId, final Optional<InventoryItem> itemOpt) {
        final var response = new HashMap<String, Object>();
        if (itemOpt.isEmpty()) {
            // Return default inventory for demo purposes
            response.put("itemId", itemId);
            response.put("name", "Item " + itemId);
            response.put("quantity", 100);
            response.put("available", true);
        } else {
            final var item = itemOpt.get();
            response.put("itemId", item.getItemId());
            response.put("name", item.getName());
            response.put("quantity", item.getQuantity());
            response.put("available", item.getQuantity() > 0);
        }
        return response;
    }

    /**
     * Configures chaos latency injection settings.
     *
     * @param enabled whether to enable latency injection
     * @param min minimum latency in milliseconds
     * @param max maximum latency in milliseconds
     * @return map containing the updated configuration
     */
    public Map<String, Object> configureChaosLatency(final Boolean enabled, final Integer min, final Integer max) {
        logger.info("Configuring chaos latency");

        if (enabled != null) {
            this.chaosLatencyEnabled = enabled;
        }
        if (min != null) {
            this.chaosLatencyMin = min;
        }
        if (max != null) {
            this.chaosLatencyMax = max;
        }

        final var response = new HashMap<String, Object>();
        response.put("enabled", chaosLatencyEnabled);
        response.put("min", chaosLatencyMin);
        response.put("max", chaosLatencyMax);
        return response;
    }

    /**
     * Configures chaos error injection settings.
     *
     * @param enabled whether to enable error injection
     * @param rate the error rate (0.0 to 1.0)
     * @return map containing the updated configuration
     */
    public Map<String, Object> configureChaosErrors(final Boolean enabled, final Double rate) {
        logger.info("Configuring chaos errors");

        if (enabled != null) {
            this.chaosErrorEnabled = enabled;
        }
        if (rate != null) {
            this.chaosErrorRate = rate;
        }

        final var response = new HashMap<String, Object>();
        response.put("enabled", chaosErrorEnabled);
        response.put("rate", chaosErrorRate);
        return response;
    }

    /**
     * Gets the current minimum chaos latency setting.
     *
     * @return the minimum latency in milliseconds
     */
    public int getChaosLatencyMin() {
        return chaosLatencyMin;
    }

    /**
     * Gets the current maximum chaos latency setting.
     *
     * @return the maximum latency in milliseconds
     */
    public int getChaosLatencyMax() {
        return chaosLatencyMax;
    }
}
