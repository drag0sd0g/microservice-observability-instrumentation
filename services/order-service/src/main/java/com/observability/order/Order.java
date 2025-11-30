package com.observability.order;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity representing an order in the system.
 * 
 * <p>Orders are created through the Order Service and stored
 * in the database with a generated UUID as the primary key.</p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String itemId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Default constructor required by JPA.
     * Initializes status to PENDING and createdAt to current time.
     */
    public Order() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    /**
     * Constructs a new Order with the specified item and quantity.
     *
     * @param itemId the ID of the item being ordered
     * @param quantity the quantity to order
     */
    public Order(final String itemId, final Integer quantity) {
        this();
        this.itemId = itemId;
        this.quantity = quantity;
    }

    /**
     * Gets the order ID.
     *
     * @return the unique order identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the order ID.
     *
     * @param id the order identifier to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Gets the item ID.
     *
     * @return the ID of the ordered item
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Sets the item ID.
     *
     * @param itemId the item identifier to set
     */
    public void setItemId(final String itemId) {
        this.itemId = itemId;
    }

    /**
     * Gets the quantity.
     *
     * @return the ordered quantity
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * Sets the quantity.
     *
     * @param quantity the quantity to set
     */
    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets the order status.
     *
     * @return the current order status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the order status.
     *
     * @param status the status to set
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return the order creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt the timestamp to set
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
