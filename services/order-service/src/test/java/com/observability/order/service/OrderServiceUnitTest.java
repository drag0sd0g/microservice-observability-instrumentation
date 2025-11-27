package com.observability.order.service;

import com.observability.order.Order;
import com.observability.order.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private Tracer tracer;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    private MeterRegistry meterRegistry;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orderService = new OrderService(orderRepository, meterRegistry, tracer);
    }

    @Test
    void createOrderSuccessfully() {
        // Arrange
        var order = new Order("item123", 5);
        order.setId("order123");

        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        var result = orderService.createOrder("item123", 5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("order123");
        assertThat(result.getItemId()).isEqualTo("item123");
        assertThat(result.getQuantity()).isEqualTo(5);
        verify(span).setAttribute("order.item_id", "item123");
        verify(span).setAttribute("order.quantity", 5);
        verify(span).end();
    }

    @Test
    void createOrderIncrementsCounter() {
        // Arrange
        var order = new Order("item123", 5);
        order.setId("order123");

        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        orderService.createOrder("item123", 5);

        // Assert
        var counter = meterRegistry.find("orders_created_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void createOrderRecordsExceptionOnFailure() {
        // Arrange
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder("item123", 5))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database error");

        verify(span).recordException(any(RuntimeException.class));
        verify(span).end();
    }

    @Test
    void getAllOrdersReturnsOrderList() {
        // Arrange
        var order1 = new Order("item1", 1);
        order1.setId("order1");
        var order2 = new Order("item2", 2);
        order2.setId("order2");

        when(orderRepository.findAll()).thenReturn(Arrays.asList(order1, order2));

        // Act
        var result = orderService.getAllOrders();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("order1");
        assertThat(result.get(1).getId()).isEqualTo("order2");
    }

    @Test
    void getAllOrdersReturnsEmptyListWhenNoOrders() {
        // Arrange
        when(orderRepository.findAll()).thenReturn(List.of());

        // Act
        var result = orderService.getAllOrders();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getOrderByIdReturnsOrderWhenExists() {
        // Arrange
        var order = new Order("item123", 5);
        order.setId("order123");

        when(orderRepository.findById("order123")).thenReturn(Optional.of(order));

        // Act
        var result = orderService.getOrderById("order123");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("order123");
    }

    @Test
    void getOrderByIdReturnsEmptyWhenNotExists() {
        // Arrange
        when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        var result = orderService.getOrderById("nonexistent");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void createOrderWithoutTracerDoesNotThrow() {
        // Arrange - create service without tracer
        var serviceWithoutTracer = new OrderService(orderRepository, meterRegistry, null);
        var order = new Order("item123", 5);
        order.setId("order123");

        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        var result = serviceWithoutTracer.createOrder("item123", 5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("order123");
    }
}
