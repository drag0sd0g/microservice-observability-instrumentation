package com.observability.gateway;

import com.observability.gateway.service.GatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayControllerUnitTest {

    @Mock
    private GatewayService gatewayService;

    private GatewayController gatewayController;

    @BeforeEach
    void setUp() {
        gatewayController = new GatewayController(gatewayService);
    }

    @Test
    void healthEndpointReturnsOk() {
        var response = gatewayController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("gateway-service");
    }

    @Test
    void createOrderReturnsCreatedOnSuccess() {
        // Arrange
        var orderRequest = new HashMap<String, Object>();
        orderRequest.put("itemId", "item123");
        orderRequest.put("quantity", 5);

        var orderResponse = new HashMap<String, Object>();
        orderResponse.put("id", "order123");
        orderResponse.put("itemId", "item123");
        orderResponse.put("quantity", 5);
        orderResponse.put("status", "PENDING");

        when(gatewayService.createOrder(anyMap())).thenReturn(orderResponse);

        // Act
        var response = gatewayController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(gatewayService).createOrder(orderRequest);
    }

    @Test
    void createOrderReturnsBadRequestOnInventoryFailure() {
        // Arrange
        var orderRequest = new HashMap<String, Object>();
        orderRequest.put("itemId", "item123");
        orderRequest.put("quantity", 5);

        when(gatewayService.createOrder(anyMap())).thenThrow(new IllegalStateException("Inventory check failed"));

        // Act
        var response = gatewayController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createOrderReturnsInternalErrorOnException() {
        // Arrange
        var orderRequest = new HashMap<String, Object>();
        orderRequest.put("itemId", "item123");
        orderRequest.put("quantity", 5);

        when(gatewayService.createOrder(anyMap())).thenThrow(new RuntimeException("Database error"));

        // Act
        var response = gatewayController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getOrdersReturnsOrderList() {
        // Arrange
        var orders = List.of(
            Map.of("id", "order1", "itemId", "item1"),
            Map.of("id", "order2", "itemId", "item2")
        );
        when(gatewayService.getOrders()).thenReturn(orders);

        // Act
        var response = gatewayController.getOrders();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(gatewayService).getOrders();
    }

    @Test
    void getOrdersReturnsInternalErrorOnException() {
        // Arrange
        when(gatewayService.getOrders()).thenThrow(new RuntimeException("Database error"));

        // Act
        var response = gatewayController.getOrders();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getOrderReturnsOrderWhenExists() {
        // Arrange
        var orderResponse = new HashMap<String, Object>();
        orderResponse.put("id", "order123");
        orderResponse.put("itemId", "item123");

        when(gatewayService.getOrder("order123")).thenReturn(orderResponse);

        // Act
        var response = gatewayController.getOrder("order123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(gatewayService).getOrder("order123");
    }

    @Test
    void getOrderReturnsNotFoundWhenNull() {
        // Arrange
        when(gatewayService.getOrder("nonexistent")).thenReturn(null);

        // Act
        var response = gatewayController.getOrder("nonexistent");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOrderReturnsBadRequestForInvalidId() {
        // Act & Assert
        assertThat(gatewayController.getOrder(null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(gatewayController.getOrder("").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(gatewayController.getOrder("   ").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(gatewayController.getOrder("a".repeat(256)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(gatewayService, never()).getOrder(anyString());
    }

    @Test
    void getOrderReturnsInternalErrorOnException() {
        // Arrange
        when(gatewayService.getOrder("order123")).thenThrow(new RuntimeException("Database error"));

        // Act
        var response = gatewayController.getOrder("order123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void checkInventoryReturnsInventoryResponse() {
        // Arrange
        var inventoryResponse = new HashMap<String, Object>();
        inventoryResponse.put("itemId", "item123");
        inventoryResponse.put("available", true);
        inventoryResponse.put("quantity", 100);

        when(gatewayService.checkInventory("item123")).thenReturn(inventoryResponse);

        // Act
        var response = gatewayController.checkInventory("item123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(gatewayService).checkInventory("item123");
    }

    @Test
    void checkInventoryReturnsBadRequestForInvalidId() {
        // Act & Assert
        assertThat(gatewayController.checkInventory(null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(gatewayController.checkInventory("").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(gatewayController.checkInventory("   ").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(gatewayController.checkInventory("a".repeat(256)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(gatewayService, never()).checkInventory(anyString());
    }

    @Test
    void checkInventoryReturnsInternalErrorOnException() {
        // Arrange
        when(gatewayService.checkInventory("item123")).thenThrow(new RuntimeException("Service error"));

        // Act
        var response = gatewayController.checkInventory("item123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void receiveAlertWebhookReturnsOk() {
        // Arrange
        var alertPayload = new HashMap<String, Object>();
        alertPayload.put("status", "firing");
        alertPayload.put("alerts", List.of(Map.of("alertname", "HighLatency")));

        doNothing().when(gatewayService).processAlertWebhook(anyMap());

        // Act
        var response = gatewayController.receiveAlertWebhook(alertPayload);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("received");
        verify(gatewayService).processAlertWebhook(alertPayload);
    }
}
