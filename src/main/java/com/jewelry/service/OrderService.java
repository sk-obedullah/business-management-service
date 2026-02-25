package com.jewelry.service;

import com.jewelry.entity.Order;
import com.jewelry.entity.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * Business logic contract for Order management.
 *
 * <p>
 * Key business rules enforced by implementations:
 * <ul>
 * <li>Creating an order decrements product stock for each line</li>
 * <li>Cancelling an order restores product stock</li>
 * <li>Orders with zero line items cannot be completed</li>
 * <li>Status transitions follow a defined FSM</li>
 * </ul>
 */
public interface OrderService {

    /**
     * Creates a new order with its line items in a single JDBC transaction.
     * Deducts stock for each product in the order lines.
     *
     * @param order fully populated Order entity with non-empty {@code lines}
     * @return the persisted order with generated id
     */
    Order createOrder(Order order);

    Optional<Order> findById(Long id);

    List<Order> findAll();

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(OrderStatus status);

    /**
     * Advances the order to {@code newStatus}.
     * Validates FSM transitions:
     * {@code PENDING → PROCESSING → COMPLETED} or any → {@code CANCELLED}.
     * Restores stock if transitioning to CANCELLED.
     */
    void updateStatus(Long orderId, OrderStatus newStatus);

    /** Loads the full order including its line items. */
    Order loadWithLines(Long orderId);
}
