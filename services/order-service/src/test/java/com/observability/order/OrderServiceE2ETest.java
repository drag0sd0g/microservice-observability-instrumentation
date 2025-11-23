package com.observability.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"otel.sdk.disabled=true"}
)
@Testcontainers
class OrderServiceE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("orders")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void containerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void healthEndpointReturnsOk() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/health", Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("order-service");
    }

    @Test
    void createAndRetrieveOrder() {
        // Create order
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("itemId", "item-abc-123");
        orderRequest.put("quantity", 10);

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "/api/orders", 
            orderRequest, 
            Map.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        
        String orderId = (String) createResponse.getBody().get("id");
        assertThat(orderId).isNotNull();
        assertThat(createResponse.getBody().get("itemId")).isEqualTo("item-abc-123");
        assertThat(createResponse.getBody().get("quantity")).isEqualTo(10);
        assertThat(createResponse.getBody().get("status")).isEqualTo("PENDING");

        // Retrieve the created order
        ResponseEntity<Map> getResponse = restTemplate.getForEntity(
            "/api/orders/" + orderId, 
            Map.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().get("id")).isEqualTo(orderId);
        assertThat(getResponse.getBody().get("itemId")).isEqualTo("item-abc-123");
    }

    @Test
    void getAllOrdersReturnsAllCreatedOrders() {
        // Create multiple orders
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("itemId", "item-" + i);
            orderRequest.put("quantity", i * 10);
            restTemplate.postForEntity("/api/orders", orderRequest, Map.class);
        }

        // Retrieve all orders
        ResponseEntity<List> response = restTemplate.getForEntity("/api/orders", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    void createOrderWithInvalidDataReturnsBadRequest() {
        // Test missing itemId
        Map<String, Object> invalidRequest1 = new HashMap<>();
        invalidRequest1.put("quantity", 10);

        ResponseEntity<Map> response1 = restTemplate.postForEntity(
            "/api/orders", 
            invalidRequest1, 
            Map.class
        );

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test missing quantity
        Map<String, Object> invalidRequest2 = new HashMap<>();
        invalidRequest2.put("itemId", "item123");

        ResponseEntity<Map> response2 = restTemplate.postForEntity(
            "/api/orders", 
            invalidRequest2, 
            Map.class
        );

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test invalid quantity (zero)
        Map<String, Object> invalidRequest3 = new HashMap<>();
        invalidRequest3.put("itemId", "item123");
        invalidRequest3.put("quantity", 0);

        ResponseEntity<Map> response3 = restTemplate.postForEntity(
            "/api/orders", 
            invalidRequest3, 
            Map.class
        );

        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test invalid quantity (negative)
        Map<String, Object> invalidRequest4 = new HashMap<>();
        invalidRequest4.put("itemId", "item123");
        invalidRequest4.put("quantity", -5);

        ResponseEntity<Map> response4 = restTemplate.postForEntity(
            "/api/orders", 
            invalidRequest4, 
            Map.class
        );

        assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getNonExistentOrderReturnsNotFound() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/orders/nonexistent-id", 
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void ordersPersistInDatabase() {
        // Create order via REST API
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("itemId", "persistent-item");
        orderRequest.put("quantity", 25);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/orders", 
            orderRequest, 
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String orderId = (String) response.getBody().get("id");

        // Verify order exists in database
        Order order = orderRepository.findById(orderId).orElse(null);
        assertThat(order).isNotNull();
        assertThat(order.getItemId()).isEqualTo("persistent-item");
        assertThat(order.getQuantity()).isEqualTo(25);
        assertThat(order.getStatus()).isEqualTo("PENDING");
        assertThat(order.getCreatedAt()).isNotNull();
    }
}
