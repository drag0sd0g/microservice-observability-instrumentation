package com.observability.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot application class for the Order Service.
 * 
 * <p>The Order Service handles order management operations including
 * order creation, retrieval, and listing.</p>
 *
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.observability.order", "com.observability.commons"})
public class OrderServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
