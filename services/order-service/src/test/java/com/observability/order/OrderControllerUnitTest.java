package com.observability.order;

import com.observability.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerUnitTest {

    @Mock
    private OrderService orderService;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(orderService);
    }

    @Test
    void healthEndpointReturnsOk() {
        ResponseEntity<Map<String, String>> response = orderController.health();
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("order-service");
    }

    @Test
    void createOrderWithValidDataReturnsCreated() {
        // Arrange
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("itemId", "item123");
        orderRequest.put("quantity", 5);

        Order savedOrder = new Order("item123", 5);
        savedOrder.setId("order123");
        
        when(orderService.createOrder("item123", 5)).thenReturn(savedOrder);

        // Act
        ResponseEntity<?> response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("id")).isEqualTo("order123");
        assertThat(body.get("itemId")).isEqualTo("item123");
        assertThat(body.get("quantity")).isEqualTo(5);
        assertThat(body.get("status")).isEqualTo("PENDING");
        
        verify(orderService).createOrder("item123", 5);
    }

    @Test
    void createOrderWithMissingItemIdReturnsBadRequest() {
        // Arrange
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("quantity", 5);

        // Act
        ResponseEntity<?> response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void createOrderWithMissingQuantityReturnsBadRequest() {
        // Arrange
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("itemId", "item123");

        // Act
        ResponseEntity<?> response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void createOrderWithZeroQuantityReturnsBadRequest() {
        // Arrange
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("itemId", "item123");
        orderRequest.put("quantity", 0);

        // Act
        ResponseEntity<?> response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void createOrderWithNegativeQuantityReturnsBadRequest() {
        // Arrange
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("itemId", "item123");
        orderRequest.put("quantity", -5);

        // Act
        ResponseEntity<?> response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void createOrderWithExcessiveQuantityReturnsBadRequest() {
        // Arrange
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("itemId", "item123");
        orderRequest.put("quantity", 10001);

        // Act
        ResponseEntity<?> response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void getAllOrdersReturnsOrderList() {
        // Arrange
        Order order1 = new Order("item1", 1);
        order1.setId("order1");
        Order order2 = new Order("item2", 2);
        order2.setId("order2");
        
        when(orderService.getAllOrders()).thenReturn(Arrays.asList(order1, order2));

        // Act
        ResponseEntity<List<Order>> response = orderController.getAllOrders();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(orderService).getAllOrders();
    }

    @Test
    void getOrderByIdReturnsOrderWhenExists() {
        // Arrange
        Order order = new Order("item123", 5);
        order.setId("order123");
        
        when(orderService.getOrderById("order123")).thenReturn(Optional.of(order));

        // Act
        ResponseEntity<?> response = orderController.getOrder("order123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Order.class);
        Order returnedOrder = (Order) response.getBody();
        assertThat(returnedOrder.getId()).isEqualTo("order123");
        verify(orderService).getOrderById("order123");
    }

    @Test
    void getOrderByIdReturnsNotFoundWhenDoesNotExist() {
        // Arrange
        when(orderService.getOrderById("nonexistent")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = orderController.getOrder("nonexistent");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(orderService).getOrderById("nonexistent");
    }

    @Test
    void getOrderWithInvalidIdReturnsBadRequest() {
        // Act
        ResponseEntity<?> response1 = orderController.getOrder(null);
        ResponseEntity<?> response2 = orderController.getOrder("");
        ResponseEntity<?> response3 = orderController.getOrder("   ");
        ResponseEntity<?> response4 = orderController.getOrder("a".repeat(256));

        // Assert
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).getOrderById(anyString());
    }
}
