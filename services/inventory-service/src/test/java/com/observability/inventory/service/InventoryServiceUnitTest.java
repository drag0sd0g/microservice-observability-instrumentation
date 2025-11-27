package com.observability.inventory.service;

import com.observability.inventory.InventoryItem;
import com.observability.inventory.InventoryRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceUnitTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private Tracer tracer;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
            inventoryRepository,
            tracer,
            false,  // chaosLatencyEnabled
            100,    // chaosLatencyMin
            2000,   // chaosLatencyMax
            false,  // chaosErrorEnabled
            0.1     // chaosErrorRate
        );
    }

    @Test
    void checkInventoryReturnsItemWhenExists() throws InterruptedException {
        // Arrange
        var item = new InventoryItem();
        item.setItemId("item123");
        item.setName("Test Item");
        item.setQuantity(50);

        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(inventoryRepository.findById("item123")).thenReturn(Optional.of(item));

        // Act
        var result = inventoryService.checkInventory("item123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("itemId")).isEqualTo("item123");
        assertThat(result.get("name")).isEqualTo("Test Item");
        assertThat(result.get("quantity")).isEqualTo(50);
        assertThat(result.get("available")).isEqualTo(true);
        verify(span).setAttribute("inventory.item_id", "item123");
        verify(span).end();
    }

    @Test
    void checkInventoryReturnsDefaultWhenItemNotFound() throws InterruptedException {
        // Arrange
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(inventoryRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        var result = inventoryService.checkInventory("nonexistent");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("itemId")).isEqualTo("nonexistent");
        assertThat(result.get("name")).isEqualTo("Item nonexistent");
        assertThat(result.get("quantity")).isEqualTo(100);
        assertThat(result.get("available")).isEqualTo(true);
    }

    @Test
    void checkInventoryReturnsUnavailableWhenQuantityIsZero() throws InterruptedException {
        // Arrange
        var item = new InventoryItem();
        item.setItemId("item123");
        item.setName("Out of Stock Item");
        item.setQuantity(0);

        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(inventoryRepository.findById("item123")).thenReturn(Optional.of(item));

        // Act
        var result = inventoryService.checkInventory("item123");

        // Assert
        assertThat(result.get("available")).isEqualTo(false);
        assertThat(result.get("quantity")).isEqualTo(0);
    }

    @Test
    void configureChaosLatencyUpdatesSettings() {
        // Act
        var result = inventoryService.configureChaosLatency(true, 200, 1000);

        // Assert
        assertThat(result.get("enabled")).isEqualTo(true);
        assertThat(result.get("min")).isEqualTo(200);
        assertThat(result.get("max")).isEqualTo(1000);
        assertThat(inventoryService.getChaosLatencyMin()).isEqualTo(200);
        assertThat(inventoryService.getChaosLatencyMax()).isEqualTo(1000);
    }

    @Test
    void configureChaosLatencyPartialUpdate() {
        // Act - only update enabled
        var result = inventoryService.configureChaosLatency(true, null, null);

        // Assert
        assertThat(result.get("enabled")).isEqualTo(true);
        assertThat(result.get("min")).isEqualTo(100);  // unchanged
        assertThat(result.get("max")).isEqualTo(2000); // unchanged
    }

    @Test
    void configureChaosErrorsUpdatesSettings() {
        // Act
        var result = inventoryService.configureChaosErrors(true, 0.5);

        // Assert
        assertThat(result.get("enabled")).isEqualTo(true);
        assertThat(result.get("rate")).isEqualTo(0.5);
    }

    @Test
    void configureChaosErrorsPartialUpdate() {
        // Act - only update enabled
        var result = inventoryService.configureChaosErrors(true, null);

        // Assert
        assertThat(result.get("enabled")).isEqualTo(true);
        assertThat(result.get("rate")).isEqualTo(0.1);  // unchanged
    }

    @Test
    void checkInventoryWithoutTracerDoesNotThrow() throws InterruptedException {
        // Arrange - create service without tracer
        var serviceWithoutTracer = new InventoryService(
            inventoryRepository,
            null,   // no tracer
            false,
            100,
            2000,
            false,
            0.1
        );

        when(inventoryRepository.findById("item123")).thenReturn(Optional.empty());

        // Act
        var result = serviceWithoutTracer.checkInventory("item123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("itemId")).isEqualTo("item123");
    }

    @Test
    void getChaosLatencyMinReturnsConfiguredValue() {
        // Act & Assert
        assertThat(inventoryService.getChaosLatencyMin()).isEqualTo(100);
    }

    @Test
    void getChaosLatencyMaxReturnsConfiguredValue() {
        // Act & Assert
        assertThat(inventoryService.getChaosLatencyMax()).isEqualTo(2000);
    }
}
