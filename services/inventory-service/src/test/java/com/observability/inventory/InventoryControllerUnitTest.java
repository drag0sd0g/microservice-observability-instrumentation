package com.observability.inventory;

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
        ResponseEntity<Map<String, String>> response = inventoryController.health();
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("inventory-service");
    }

    @Test
    void checkInventoryReturnsItemWhenExists() throws Exception {
        // Arrange
        Map<String, Object> inventoryResponse = new HashMap<>();
        inventoryResponse.put("itemId", "item123");
        inventoryResponse.put("name", "Test Item");
        inventoryResponse.put("quantity", 50);
        inventoryResponse.put("available", true);
        
        when(inventoryService.checkInventory("item123")).thenReturn(inventoryResponse);

        // Act
        ResponseEntity<?> response = inventoryController.checkInventory("item123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("itemId")).isEqualTo("item123");
        assertThat(body.get("name")).isEqualTo("Test Item");
        assertThat(body.get("quantity")).isEqualTo(50);
        assertThat(body.get("available")).isEqualTo(true);
        
        verify(inventoryService).checkInventory("item123");
    }

    @Test
    void checkInventoryReturnsDefaultWhenItemDoesNotExist() throws Exception {
        // Arrange
        Map<String, Object> defaultResponse = new HashMap<>();
        defaultResponse.put("itemId", "nonexistent");
        defaultResponse.put("name", "Item nonexistent");
        defaultResponse.put("quantity", 100);
        defaultResponse.put("available", true);
        
        when(inventoryService.checkInventory("nonexistent")).thenReturn(defaultResponse);

        // Act
        ResponseEntity<?> response = inventoryController.checkInventory("nonexistent");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("itemId")).isEqualTo("nonexistent");
        assertThat(body.get("name")).isEqualTo("Item nonexistent");
        assertThat(body.get("quantity")).isEqualTo(100);
        assertThat(body.get("available")).isEqualTo(true);
        
        verify(inventoryService).checkInventory("nonexistent");
    }

    @Test
    void checkInventoryReturnsUnavailableWhenQuantityIsZero() throws Exception {
        // Arrange
        Map<String, Object> inventoryResponse = new HashMap<>();
        inventoryResponse.put("itemId", "item123");
        inventoryResponse.put("name", "Out of Stock Item");
        inventoryResponse.put("quantity", 0);
        inventoryResponse.put("available", false);
        
        when(inventoryService.checkInventory("item123")).thenReturn(inventoryResponse);

        // Act
        ResponseEntity<?> response = inventoryController.checkInventory("item123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("available")).isEqualTo(false);
        assertThat(body.get("quantity")).isEqualTo(0);
    }

    @Test
    void checkInventoryWithInvalidIdReturnsBadRequest() throws Exception {
        // Act
        ResponseEntity<?> response1 = inventoryController.checkInventory(null);
        ResponseEntity<?> response2 = inventoryController.checkInventory("");
        ResponseEntity<?> response3 = inventoryController.checkInventory("   ");
        ResponseEntity<?> response4 = inventoryController.checkInventory("a".repeat(256));

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
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", true);
        config.put("min", 200);
        config.put("max", 1000);

        Map<String, Object> serviceResponse = new HashMap<>();
        serviceResponse.put("enabled", true);
        serviceResponse.put("min", 200);
        serviceResponse.put("max", 1000);

        when(inventoryService.configureChaosLatency(true, 200, 1000)).thenReturn(serviceResponse);
        when(inventoryService.getChaosLatencyMin()).thenReturn(200);
        when(inventoryService.getChaosLatencyMax()).thenReturn(1000);

        // Act
        ResponseEntity<Map<String, Object>> response = inventoryController.configureChaosLatency(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("enabled")).isEqualTo(true);
        assertThat(response.getBody().get("min")).isEqualTo(200);
        assertThat(response.getBody().get("max")).isEqualTo(1000);
    }

    @Test
    void configureChaosLatencyRejectsInvalidMin() {
        // Arrange
        Map<String, Object> config = new HashMap<>();
        config.put("min", -10);

        // Act
        ResponseEntity<Map<String, Object>> response = inventoryController.configureChaosLatency(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void configureChaosLatencyRejectsInvalidMax() {
        // Arrange
        Map<String, Object> config = new HashMap<>();
        config.put("max", 20000);

        // Act
        ResponseEntity<Map<String, Object>> response = inventoryController.configureChaosLatency(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void configureChaosLatencyRejectsMinGreaterThanMax() {
        // Arrange
        Map<String, Object> config = new HashMap<>();
        config.put("min", 2000);
        config.put("max", 1000);

        Map<String, Object> serviceResponse = new HashMap<>();
        serviceResponse.put("enabled", false);
        serviceResponse.put("min", 2000);
        serviceResponse.put("max", 1000);

        when(inventoryService.configureChaosLatency(null, 2000, 1000)).thenReturn(serviceResponse);
        when(inventoryService.getChaosLatencyMin()).thenReturn(2000);
        when(inventoryService.getChaosLatencyMax()).thenReturn(1000);

        // Act
        ResponseEntity<Map<String, Object>> response = inventoryController.configureChaosLatency(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void configureChaosErrorsUpdatesSettings() {
        // Arrange
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", true);
        config.put("rate", 0.2);

        Map<String, Object> serviceResponse = new HashMap<>();
        serviceResponse.put("enabled", true);
        serviceResponse.put("rate", 0.2);

        when(inventoryService.configureChaosErrors(true, 0.2)).thenReturn(serviceResponse);

        // Act
        ResponseEntity<Map<String, Object>> response = inventoryController.configureChaosErrors(config);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("enabled")).isEqualTo(true);
        assertThat(response.getBody().get("rate")).isEqualTo(0.2);
    }

    @Test
    void configureChaosErrorsRejectsInvalidRate() {
        // Arrange
        Map<String, Object> config1 = new HashMap<>();
        config1.put("rate", -0.1);

        Map<String, Object> config2 = new HashMap<>();
        config2.put("rate", 1.5);

        // Act
        ResponseEntity<Map<String, Object>> response1 = inventoryController.configureChaosErrors(config1);
        ResponseEntity<Map<String, Object>> response2 = inventoryController.configureChaosErrors(config2);

        // Assert
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
