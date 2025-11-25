package com.observability.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
public class InventoryServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceApplication.class);
    private static final Pattern VALID_DB_NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");
    private static final Pattern JDBC_URL_DB_PATTERN = Pattern.compile("(jdbc:postgresql://[^/]+/)([^?]+)(.*)");

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

        System.out.println("InventoryService: Creating database '" + databaseName + "' if it does not exist...");
        logger.info("Creating database '{}' if it does not exist...", databaseName);

        int maxRetries = 30;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try (Connection conn = DriverManager.getConnection(initUrl, username, password)) {
                conn.setAutoCommit(true);
                
                // Use CREATE DATABASE IF NOT EXISTS for CockroachDB compatibility
                // Database name is validated above, safe to use in DDL
                try (Statement createStmt = conn.createStatement()) {
                    createStmt.execute("CREATE DATABASE IF NOT EXISTS " + databaseName);
                    System.out.println("InventoryService: CREATE DATABASE statement executed for '" + databaseName + "'");
                    logger.info("CREATE DATABASE statement executed for '{}'", databaseName);
                }
                
                // Build verification URL by replacing database name in the URL
                String verifyUrl = buildDatabaseUrl(initUrl, databaseName);
                
                // Verify the database was created by checking if we can connect to it
                try (Connection verifyConn = DriverManager.getConnection(verifyUrl, username, password);
                     Statement verifyStmt = verifyConn.createStatement();
                     ResultSet rs = verifyStmt.executeQuery("SELECT 1")) {
                    if (rs.next() && rs.getInt(1) == 1) {
                        System.out.println("InventoryService: Database '" + databaseName + "' verified and ready.");
                        logger.info("Database '{}' verified and ready.", databaseName);
                    } else {
                        throw new RuntimeException("Database verification failed: unexpected query result");
                    }
                }
                return; // Success
            } catch (Exception e) {
                retryCount++;
                System.out.println("InventoryService: Attempt " + retryCount + "/" + maxRetries + " - Failed: " + e.getMessage());
                logger.warn("Attempt {}/{} - Failed to initialize database: {}", retryCount, maxRetries, e.getMessage());
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
    
    private static String buildDatabaseUrl(String initUrl, String databaseName) {
        Matcher matcher = JDBC_URL_DB_PATTERN.matcher(initUrl);
        if (matcher.matches()) {
            // Replace the database name part of the URL
            return matcher.group(1) + databaseName + matcher.group(3);
        }
        // Fallback: simple string replacement for common patterns
        throw new IllegalArgumentException("Could not parse JDBC URL to replace database name: " + initUrl);
    }
}
