package com.observability.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for Order entities.
 * 
 * <p>Provides standard CRUD operations for orders persisted
 * in the database.</p>
 *
 * @since 1.0.0
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
}
