package com.observability.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.regex.Pattern;

@SpringBootApplication
public class InventoryServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceApplication.class);
    private static final Pattern VALID_DB_NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

    public static void main(String[] args) {
        // Initialize database before Spring context starts
        initializeDatabase();
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    private static void initializeDatabase() {
        String initUrl = System.getenv().getOrDefault("DATABASE_INIT_URL", 
            "jdbc:postgresql://cockroachdb:26257/defaultdb?sslmode=disable");
        String username = System.getenv().getOrDefault("DATABASE_INIT_USERNAME", "root");
        String password = System.getenv().getOrDefault("DATABASE_INIT_PASSWORD", "");
        String databaseName = "inventory";

        // Validate database name to prevent SQL injection
        if (!VALID_DB_NAME.matcher(databaseName).matches()) {
            throw new IllegalArgumentException("Invalid database name: " + databaseName);
        }

        logger.info("Creating database '{}' if it does not exist...", databaseName);

        int maxRetries = 30;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try (Connection conn = DriverManager.getConnection(initUrl, username, password)) {
                // Use CREATE DATABASE IF NOT EXISTS for CockroachDB compatibility
                // Database name is validated above, safe to use in DDL
                try (Statement createStmt = conn.createStatement()) {
                    createStmt.execute("CREATE DATABASE IF NOT EXISTS " + databaseName);
                    logger.info("Database '{}' is ready.", databaseName);
                }
                return; // Success
            } catch (Exception e) {
                retryCount++;
                logger.warn("Attempt {}/{} - Failed to connect to database: {}", retryCount, maxRetries, e.getMessage());
                if (retryCount >= maxRetries) {
                    logger.error("Failed to initialize database '{}' after {} attempts", databaseName, maxRetries);
                    throw new RuntimeException("Database initialization failed", e);
                }
                try {
                    Thread.sleep(2000); // Wait 2 seconds before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
