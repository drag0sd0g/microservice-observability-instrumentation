package com.observability.order;

import com.observability.order.model.CreateOrderRequest;
import com.observability.order.model.ErrorResponse;
import com.observability.order.model.OrderResponse;
import com.observability.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
        var response = orderController.health();
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("UP");
        assertThat(response.getBody().getService()).isEqualTo("order-service");
    }

    @Test
    void createOrderWithValidDataReturnsCreated() {
        // Arrange
        var orderRequest = new CreateOrderRequest("item123", 5);

        var savedOrder = new Order("item123", 5);
        savedOrder.setId("order123");
        
        when(orderService.createOrder("item123", 5)).thenReturn(savedOrder);

        // Act
        var response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(OrderResponse.class);
        
        var body = (OrderResponse) response.getBody();
        assertThat(body.getId()).isEqualTo("order123");
        assertThat(body.getItemId()).isEqualTo("item123");
        assertThat(body.getQuantity()).isEqualTo(5);
        assertThat(body.getStatus()).isEqualTo(OrderResponse.StatusEnum.PENDING);
        
        verify(orderService).createOrder("item123", 5);
    }

    @Test
    void createOrderWithMissingItemIdReturnsBadRequest() {
        // Arrange
        var orderRequest = new CreateOrderRequest();
        orderRequest.setQuantity(5);

        // Act
        var response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void createOrderWithMissingQuantityReturnsBadRequest() {
        // Arrange
        var orderRequest = new CreateOrderRequest();
        orderRequest.setItemId("item123");

        // Act
        var response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void createOrderWithZeroQuantityReturnsBadRequest() {
        // Arrange
        var orderRequest = new CreateOrderRequest("item123", 0);

        // Act
        var response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void createOrderWithNegativeQuantityReturnsBadRequest() {
        // Arrange
        var orderRequest = new CreateOrderRequest("item123", -5);

        // Act
        var response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void createOrderWithExcessiveQuantityReturnsBadRequest() {
        // Arrange
        var orderRequest = new CreateOrderRequest("item123", 10001);

        // Act
        var response = orderController.createOrder(orderRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).createOrder(anyString(), anyInt());
    }

    @Test
    void getAllOrdersReturnsOrderList() {
        // Arrange
        var order1 = new Order("item1", 1);
        order1.setId("order1");
        var order2 = new Order("item2", 2);
        order2.setId("order2");
        
        when(orderService.getAllOrders()).thenReturn(Arrays.asList(order1, order2));

        // Act
        var response = orderController.getAllOrders();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(orderService).getAllOrders();
    }

    @Test
    void getOrderByIdReturnsOrderWhenExists() {
        // Arrange
        var order = new Order("item123", 5);
        order.setId("order123");
        
        when(orderService.getOrderById("order123")).thenReturn(Optional.of(order));

        // Act
        var response = orderController.getOrder("order123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(OrderResponse.class);
        var returnedOrder = (OrderResponse) response.getBody();
        assertThat(returnedOrder.getId()).isEqualTo("order123");
        verify(orderService).getOrderById("order123");
    }

    @Test
    void getOrderByIdReturnsNotFoundWhenDoesNotExist() {
        // Arrange
        when(orderService.getOrderById("nonexistent")).thenReturn(Optional.empty());

        // Act
        var response = orderController.getOrder("nonexistent");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(orderService).getOrderById("nonexistent");
    }

    @Test
    void getOrderWithInvalidIdReturnsBadRequest() {
        // Act
        var response1 = orderController.getOrder(null);
        var response2 = orderController.getOrder("");
        var response3 = orderController.getOrder("   ");
        var response4 = orderController.getOrder("a".repeat(256));

        // Assert
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderService, never()).getOrderById(anyString());
    }
}
