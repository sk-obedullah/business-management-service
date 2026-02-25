package com.jewelry.repository;

import com.jewelry.entity.Order;
import com.jewelry.entity.OrderStatus;

import java.util.List;

/**
 * Repository contract for {@link Order} persistence.
 * Extends {@link BaseRepository} with order-domain queries.
 */
public interface OrderRepository extends BaseRepository<Order, Long> {

    /** Returns orders for a specific customer, newest first. */
    List<Order> findByCustomerId(Long customerId);

    /** Returns orders filtered by status. */
    List<Order> findByStatus(OrderStatus status);

    /** Updates only the order status column. */
    void updateStatus(Long orderId, OrderStatus newStatus);

    /** Updates total_amount (recalculated after line changes). */
    void updateTotal(Long orderId, java.math.BigDecimal totalAmount);
}
