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

import static com.observability.inventory.util.LogUtils.sanitizeForLog;

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

    public InventoryService(
            InventoryRepository inventoryRepository,
            @Autowired(required = false) Tracer tracer,
            @Value("${chaos.latency.enabled:false}") boolean chaosLatencyEnabled,
            @Value("${chaos.latency.min:100}") int chaosLatencyMin,
            @Value("${chaos.latency.max:2000}") int chaosLatencyMax,
            @Value("${chaos.error.enabled:false}") boolean chaosErrorEnabled,
            @Value("${chaos.error.rate:0.1}") double chaosErrorRate) {
        this.inventoryRepository = inventoryRepository;
        this.tracer = tracer;
        this.chaosLatencyEnabled = chaosLatencyEnabled;
        this.chaosLatencyMin = chaosLatencyMin;
        this.chaosLatencyMax = chaosLatencyMax;
        this.chaosErrorEnabled = chaosErrorEnabled;
        this.chaosErrorRate = chaosErrorRate;
    }

    public Map<String, Object> checkInventory(String itemId) throws InterruptedException {
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
                int delay = random.nextInt(chaosLatencyMax - chaosLatencyMin) + chaosLatencyMin;
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

            Optional<InventoryItem> itemOpt = inventoryRepository.findById(itemId);

            Map<String, Object> response = new HashMap<>();
            if (itemOpt.isEmpty()) {
                // Return default inventory for demo purposes
                response.put("itemId", itemId);
                response.put("name", "Item " + itemId);
                response.put("quantity", 100);
                response.put("available", true);
            } else {
                InventoryItem item = itemOpt.get();
                response.put("itemId", item.getItemId());
                response.put("name", item.getName());
                response.put("quantity", item.getQuantity());
                response.put("available", item.getQuantity() > 0);
            }

            return response;
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    public Map<String, Object> configureChaosLatency(Boolean enabled, Integer min, Integer max) {
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

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", chaosLatencyEnabled);
        response.put("min", chaosLatencyMin);
        response.put("max", chaosLatencyMax);
        return response;
    }

    public Map<String, Object> configureChaosErrors(Boolean enabled, Double rate) {
        logger.info("Configuring chaos errors");

        if (enabled != null) {
            this.chaosErrorEnabled = enabled;
        }
        if (rate != null) {
            this.chaosErrorRate = rate;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", chaosErrorEnabled);
        response.put("rate", chaosErrorRate);
        return response;
    }

    // Getters for validation in controller
    public int getChaosLatencyMin() {
        return chaosLatencyMin;
    }

    public int getChaosLatencyMax() {
        return chaosLatencyMax;
    }
}
