package com.observability.inventory;

import com.observability.inventory.model.*;
import com.observability.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerUnitTest {

    @Mock
    private InventoryService inventoryService;

    private InventoryController inventoryController;

    @BeforeEach
    void setUp() {
        inventoryController = new InventoryController(inventoryService);
    }

    @Test
    void healthEndpointReturnsOk() {
        var response = inventoryController.health();
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("UP");
        assertThat(response.getBody().getService()).isEqualTo("inventory-service");
    }

    @Test
    void checkInventoryReturnsItemWhenExists() throws Exception {
        // Arrange
        var inventoryResponse = new HashMap<String, Object>();
        inventoryResponse.put("itemId", "item123");
        inventoryResponse.put("name", "Test Item");
        inventoryResponse.put("quantity", 50);
        inventoryResponse.put("available", true);
        
        when(inventoryService.checkInventory("item123")).thenReturn(inventoryResponse);

        // Act
        var response = inventoryController.checkInventory("item123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(InventoryResponse.class);
        
        var body = (InventoryResponse) response.getBody();
        assertThat(body.getItemId()).isEqualTo("item123");
        assertThat(body.getName()).isEqualTo("Test Item");
        assertThat(body.getQuantity()).isEqualTo(50);
        assertThat(body.getAvailable()).isEqualTo(true);
        
        verify(inventoryService).checkInventory("item123");
    }

    @Test
    void checkInventoryReturnsDefaultWhenItemDoesNotExist() throws Exception {
        // Arrange
        var defaultResponse = new HashMap<String, Object>();
        defaultResponse.put("itemId", "nonexistent");
        defaultResponse.put("name", "Item nonexistent");
        defaultResponse.put("quantity", 100);
        defaultResponse.put("available", true);
        
        when(inventoryService.checkInventory("nonexistent")).thenReturn(defaultResponse);

        // Act
        var response = inventoryController.checkInventory("nonexistent");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(InventoryResponse.class);
        
        var body = (InventoryResponse) response.getBody();
        assertThat(body.getItemId()).isEqualTo("nonexistent");
        assertThat(body.getName()).isEqualTo("Item nonexistent");
        assertThat(body.getQuantity()).isEqualTo(100);
        assertThat(body.getAvailable()).isEqualTo(true);
        
        verify(inventoryService).checkInventory("nonexistent");
    }

    @Test
    void checkInventoryReturnsUnavailableWhenQuantityIsZero() throws Exception {
        // Arrange
        var inventoryResponse = new HashMap<String, Object>();
        inventoryResponse.put("itemId", "item123");
        inventoryResponse.put("name", "Out of Stock Item");
        inventoryResponse.put("quantity", 0);
        inventoryResponse.put("available", false);
        
        when(inventoryService.checkInventory("item123")).thenReturn(inventoryResponse);

        // Act
        var response = inventoryController.checkInventory("item123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        var body = (InventoryResponse) response.getBody();
        assertThat(body.getAvailable()).isEqualTo(false);
        assertThat(body.getQuantity()).isEqualTo(0);
    }

    @Test
    void checkInventoryWithInvalidIdReturnsBadRequest() throws Exception {
        // Act
        var response1 = inventoryController.checkInventory(null);
        var response2 = inventoryController.checkInventory("");
        var response3 = inventoryController.checkInventory("   ");
        var response4 = inventoryController.checkInventory("a".repeat(256));

        // Assert
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(inventoryService, never()).checkInventory(anyString());
    }

    @Test
    void configureChaosLatencyUpdatesSettings() {
        // Arrange
        var config = new ChaosLatencyRequest()
            .enabled(true)
            .min(200)
            .max(1000);

        var serviceResponse = new HashMap<String, Object>();
        serviceResponse.put("enabled", true);
        serviceResponse.put("min", 200);
        serviceResponse.put("max", 1000);

        when(inventoryService.configureChaosLatency(true, 200, 1000)).thenReturn(serviceResponse);
        when(inventoryService.getChaosLatencyMin()).thenReturn(200);
        when(inventoryService.getChaosLatencyMax()).thenReturn(1000);

        // Act
        var response = inventoryController.configureChaosLatency(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ChaosLatencyResponse.class);
        var body = (ChaosLatencyResponse) response.getBody();
        assertThat(body.getEnabled()).isEqualTo(true);
        assertThat(body.getMin()).isEqualTo(200);
        assertThat(body.getMax()).isEqualTo(1000);
    }

    @Test
    void configureChaosLatencyRejectsInvalidMin() {
        // Arrange
        var config = new ChaosLatencyRequest().min(-10);

        // Act
        var response = inventoryController.configureChaosLatency(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void configureChaosLatencyRejectsInvalidMax() {
        // Arrange
        var config = new ChaosLatencyRequest().max(20000);

        // Act
        var response = inventoryController.configureChaosLatency(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void configureChaosLatencyRejectsMinGreaterThanMax() {
        // Arrange
        var config = new ChaosLatencyRequest()
            .min(2000)
            .max(1000);

        var serviceResponse = new HashMap<String, Object>();
        serviceResponse.put("enabled", false);
        serviceResponse.put("min", 2000);
        serviceResponse.put("max", 1000);

        when(inventoryService.configureChaosLatency(null, 2000, 1000)).thenReturn(serviceResponse);
        when(inventoryService.getChaosLatencyMin()).thenReturn(2000);
        when(inventoryService.getChaosLatencyMax()).thenReturn(1000);

        // Act
        var response = inventoryController.configureChaosLatency(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void configureChaosErrorsUpdatesSettings() {
        // Arrange
        var config = new ChaosErrorRequest()
            .enabled(true)
            .rate(0.2);

        var serviceResponse = new HashMap<String, Object>();
        serviceResponse.put("enabled", true);
        serviceResponse.put("rate", 0.2);

        when(inventoryService.configureChaosErrors(true, 0.2)).thenReturn(serviceResponse);

        // Act
        var response = inventoryController.configureChaosErrors(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ChaosErrorResponse.class);
        var body = (ChaosErrorResponse) response.getBody();
        assertThat(body.getEnabled()).isEqualTo(true);
        assertThat(body.getRate()).isEqualTo(0.2);
    }

    @Test
    void configureChaosErrorsRejectsInvalidRate() {
        // Arrange
        var config1 = new ChaosErrorRequest().rate(-0.1);
        var config2 = new ChaosErrorRequest().rate(1.5);

        // Act
        var response1 = inventoryController.configureChaosErrors(config1);
        var response2 = inventoryController.configureChaosErrors(config2);

        // Assert
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
