package com.observability.inventory;

import jakarta.persistence.*;

/**
 * JPA Entity representing an inventory item in the system.
 * 
 * <p>Inventory items track the available stock quantity for
 * each item that can be ordered.</p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "inventory")
public class InventoryItem {

    @Id
    private String itemId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String name;

    /**
     * Gets the item ID.
     *
     * @return the unique item identifier
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
     * Gets the available quantity.
     *
     * @return the available quantity
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * Sets the available quantity.
     *
     * @param quantity the quantity to set
     */
    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets the item name.
     *
     * @return the item name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the item name.
     *
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }
}
