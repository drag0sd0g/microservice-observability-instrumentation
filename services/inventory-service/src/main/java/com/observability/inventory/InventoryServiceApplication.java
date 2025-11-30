package com.observability.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot application class for the Inventory Service.
 * 
 * <p>The Inventory Service handles inventory management operations
 * including stock checks and availability queries. It also provides
 * chaos engineering capabilities for testing system resilience.</p>
 *
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.observability.inventory", "com.observability.commons"})
public class InventoryServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
