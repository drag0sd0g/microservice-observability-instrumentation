package com.observability.inventory;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class InventoryItem {

    @Id
    private String itemId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String name;

    public InventoryItem() {
    }

    public InventoryItem(String itemId, String name, Integer quantity) {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
    }

    // Getters and setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
