package com.observability.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerUnitTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderController orderController;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orderController = new OrderController(meterRegistry);
        ReflectionTestUtils.setField(orderController, "orderRepository", orderRepository);
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
        
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

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
        
        verify(orderRepository).save(any(Order.class));
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
        verify(orderRepository, never()).save(any(Order.class));
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
        verify(orderRepository, never()).save(any(Order.class));
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
        verify(orderRepository, never()).save(any(Order.class));
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
        verify(orderRepository, never()).save(any(Order.class));
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
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getAllOrdersReturnsOrderList() {
        // Arrange
        Order order1 = new Order("item1", 1);
        order1.setId("order1");
        Order order2 = new Order("item2", 2);
        order2.setId("order2");
        
        when(orderRepository.findAll()).thenReturn(Arrays.asList(order1, order2));

        // Act
        ResponseEntity<List<Order>> response = orderController.getAllOrders();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(orderRepository).findAll();
    }

    @Test
    void getOrderByIdReturnsOrderWhenExists() {
        // Arrange
        Order order = new Order("item123", 5);
        order.setId("order123");
        
        when(orderRepository.findById("order123")).thenReturn(Optional.of(order));

        // Act
        ResponseEntity<?> response = orderController.getOrder("order123");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Order.class);
        Order returnedOrder = (Order) response.getBody();
        assertThat(returnedOrder.getId()).isEqualTo("order123");
        verify(orderRepository).findById("order123");
    }

    @Test
    void getOrderByIdReturnsNotFoundWhenDoesNotExist() {
        // Arrange
        when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = orderController.getOrder("nonexistent");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(orderRepository).findById("nonexistent");
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
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void createOrderIncrementsMetricsCounter() {
        // Arrange
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("itemId", "item123");
        orderRequest.put("quantity", 5);

        Order savedOrder = new Order("item123", 5);
        savedOrder.setId("order123");
        
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        double counterBefore = meterRegistry.counter("orders_created_total").count();

        // Act
        orderController.createOrder(orderRequest);

        // Assert
        double counterAfter = meterRegistry.counter("orders_created_total").count();
        assertThat(counterAfter).isEqualTo(counterBefore + 1);
    }
}
