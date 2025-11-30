package com.observability.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for InventoryItem entities.
 * 
 * <p>Provides standard CRUD operations for inventory items
 * persisted in the database.</p>
 *
 * @since 1.0.0
 */
@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, String> {
}
