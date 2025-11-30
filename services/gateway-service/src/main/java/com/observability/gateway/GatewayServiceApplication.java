package com.observability.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Main Spring Boot application class for the Gateway Service.
 * 
 * <p>The Gateway Service acts as the API gateway for the microservices
 * architecture, routing requests to downstream services and providing
 * a unified entry point for clients.</p>
 *
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.observability.gateway", "com.observability.commons"})
public class GatewayServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    /**
     * Creates a WebClient.Builder bean for making HTTP requests to downstream services.
     *
     * @return a WebClient.Builder instance
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
