package com.jewelry.repository;

import com.jewelry.entity.OrderLine;

import java.util.List;

/**
 * Repository contract for {@link OrderLine} persistence.
 * Note: lines are always owned by an Order — no standalone findAll.
 */
public interface OrderLineRepository {

    /** Inserts a single line and returns it with its generated id. */
    OrderLine save(OrderLine line);

    /** Returns all lines belonging to the given order. */
    List<OrderLine> findByOrderId(Long orderId);

    /** Deletes all lines for an order (used when cancelling/rewriting). */
    void deleteByOrderId(Long orderId);
}
