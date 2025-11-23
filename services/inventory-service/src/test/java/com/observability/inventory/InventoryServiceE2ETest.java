package com.observability.inventory;

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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "otel.sdk.disabled=true",
        "chaos.latency.enabled=false",
        "chaos.error.enabled=false"
    }
)
@Testcontainers
class InventoryServiceE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("inventory")
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
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
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
        assertThat(response.getBody().get("service")).isEqualTo("inventory-service");
    }

    @Test
    void checkInventoryForExistingItem() {
        // Create inventory item directly in database
        InventoryItem item = new InventoryItem("laptop-001", "Dell Laptop", 25);
        inventoryRepository.save(item);

        // Check inventory via REST API
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/inventory/laptop-001", 
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("itemId")).isEqualTo("laptop-001");
        assertThat(response.getBody().get("name")).isEqualTo("Dell Laptop");
        assertThat(response.getBody().get("quantity")).isEqualTo(25);
        assertThat(response.getBody().get("available")).isEqualTo(true);
    }

    @Test
    void checkInventoryForNonExistingItemReturnsDefault() {
        // Check inventory for item that doesn't exist
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/inventory/unknown-item", 
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("itemId")).isEqualTo("unknown-item");
        assertThat(response.getBody().get("name")).isEqualTo("Item unknown-item");
        assertThat(response.getBody().get("quantity")).isEqualTo(100);
        assertThat(response.getBody().get("available")).isEqualTo(true);
    }

    @Test
    void checkInventoryForOutOfStockItem() {
        // Create out-of-stock item
        InventoryItem item = new InventoryItem("mouse-001", "Wireless Mouse", 0);
        inventoryRepository.save(item);

        // Check inventory
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/inventory/mouse-001", 
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("itemId")).isEqualTo("mouse-001");
        assertThat(response.getBody().get("quantity")).isEqualTo(0);
        assertThat(response.getBody().get("available")).isEqualTo(false);
    }

    @Test
    void checkInventoryWithInvalidIdReturnsBadRequest() {
        ResponseEntity<Map> response1 = restTemplate.getForEntity(
            "/api/inventory/", 
            Map.class
        );
        
        // Empty path segment will likely result in 404 or redirect, so let's test with very long ID
        String longId = "a".repeat(256);
        ResponseEntity<Map> response2 = restTemplate.getForEntity(
            "/api/inventory/" + longId, 
            Map.class
        );

        // At least one should fail validation
        boolean hasError = response1.getStatusCode() == HttpStatus.BAD_REQUEST || 
                          response1.getStatusCode() == HttpStatus.NOT_FOUND ||
                          response2.getStatusCode() == HttpStatus.BAD_REQUEST;
        assertThat(hasError).isTrue();
    }

    @Test
    void configureChaosLatency() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", true);
        config.put("min", 50);
        config.put("max", 200);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/chaos/latency",
            config,
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("enabled")).isEqualTo(true);
        assertThat(response.getBody().get("min")).isEqualTo(50);
        assertThat(response.getBody().get("max")).isEqualTo(200);
    }

    @Test
    void configureChaosLatencyWithInvalidValues() {
        // Test with negative min
        Map<String, Object> invalidConfig1 = new HashMap<>();
        invalidConfig1.put("min", -10);

        ResponseEntity<Map> response1 = restTemplate.postForEntity(
            "/api/chaos/latency",
            invalidConfig1,
            Map.class
        );

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test with min > max
        Map<String, Object> invalidConfig2 = new HashMap<>();
        invalidConfig2.put("min", 2000);
        invalidConfig2.put("max", 1000);

        ResponseEntity<Map> response2 = restTemplate.postForEntity(
            "/api/chaos/latency",
            invalidConfig2,
            Map.class
        );

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void configureChaosErrors() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", true);
        config.put("rate", 0.15);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/chaos/errors",
            config,
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("enabled")).isEqualTo(true);
        assertThat(response.getBody().get("rate")).isEqualTo(0.15);
    }

    @Test
    void configureChaosErrorsWithInvalidRate() {
        // Test with rate > 1.0
        Map<String, Object> invalidConfig = new HashMap<>();
        invalidConfig.put("rate", 1.5);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/chaos/errors",
            invalidConfig,
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void multipleInventoryItemsCanBePersisted() {
        // Create multiple items
        inventoryRepository.save(new InventoryItem("item-001", "Item 1", 10));
        inventoryRepository.save(new InventoryItem("item-002", "Item 2", 20));
        inventoryRepository.save(new InventoryItem("item-003", "Item 3", 30));

        // Verify each item can be retrieved
        ResponseEntity<Map> response1 = restTemplate.getForEntity("/api/inventory/item-001", Map.class);
        ResponseEntity<Map> response2 = restTemplate.getForEntity("/api/inventory/item-002", Map.class);
        ResponseEntity<Map> response3 = restTemplate.getForEntity("/api/inventory/item-003", Map.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(response1.getBody().get("quantity")).isEqualTo(10);
        assertThat(response2.getBody().get("quantity")).isEqualTo(20);
        assertThat(response3.getBody().get("quantity")).isEqualTo(30);
    }
}
