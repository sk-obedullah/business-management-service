package com.jewelry.service.impl;

import com.jewelry.entity.Order;
import com.jewelry.entity.OrderLine;
import com.jewelry.entity.OrderStatus;
import com.jewelry.entity.Product;
import com.jewelry.exception.EntityNotFoundException;
import com.jewelry.exception.ServiceException;
import com.jewelry.repository.OrderLineRepository;
import com.jewelry.repository.OrderRepository;
import com.jewelry.repository.ProductRepository;
import com.jewelry.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Production implementation of {@link OrderService}.
 *
 * <p>All previously manual JDBC transaction management is replaced by
 * {@code @Transactional} — Spring rolls back automatically on any
 * unchecked exception.
 *
 * <p><strong>Business rules enforced:</strong>
 * <ol>
 * <li>Transactional order creation — header, lines, and stock decrements committed atomically</li>
 * <li>Price snapshot — unit_price and cost_price copied from Product at creation time</li>
 * <li>Stock deduction on create — each line decrements product.quantity_on_hand</li>
 * <li>Stock restoration on cancel — transitioning to CANCELLED increments stock back</li>
 * <li>FSM enforcement — only valid status transitions allowed</li>
 * </ol>
 */
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Order createOrder(Order order) {
        if (order == null)
            throw new IllegalArgumentException("Order must not be null");
        if (order.getCustomerId() == null)
            throw new IllegalArgumentException("Customer is required");
        if (order.getLines() == null || order.getLines().isEmpty())
            throw new IllegalArgumentException("Order must have at least one item");

        // ── 1. Validate products & build price snapshots ──────────────────────
        for (OrderLine line : order.getLines()) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product", line.getProductId()));

            if (product.getQuantityOnHand() < line.getQuantity()) {
                throw new ServiceException(
                        "Insufficient stock for '" + product.getName() + "'. "
                                + "Available: " + product.getQuantityOnHand()
                                + ", Requested: " + line.getQuantity());
            }

            // Snapshot prices at time of order
            line.setUnitPrice(product.getSellingPrice());
            line.setCostPrice(product.getCostPrice());
            line.setProductName(product.getName());
            line.setProductSku(product.getSku());
            // Link the JPA relationship
            line.setProduct(product);
        }

        // ── 2. Compute order total ─────────────────────────────────────────────
        BigDecimal total = order.getLines().stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        if (order.getStatus() == null) order.setStatus(OrderStatus.PENDING);
        if (order.getDiscount() == null) order.setDiscount(BigDecimal.ZERO);

        // ── 3. Link lines to order for cascade save ───────────────────────────
        for (OrderLine line : order.getLines()) {
            line.setOrder(order);
        }

        // ── 4. Save — Spring @Transactional ensures full rollback on failure ───
        // orderRepository.save() cascades to lines via CascadeType.ALL on Order.lines
        Order saved = orderRepository.save(order);

        // ── 5. Decrement stock using JPA ──────────────────────────────────────
        for (OrderLine line : saved.getLines()) {
            Product product = line.getProduct();
            product.setQuantityOnHand(product.getQuantityOnHand() - line.getQuantity());
            productRepository.save(product);
        }

        log.info("Order created id={} total={}", saved.getId(), total);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Order loadWithLines(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
    }

    @Override
    public void updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        validateTransition(order.getStatus(), newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            // Restore stock for every line — all within the same @Transactional context
            for (OrderLine line : order.getLines()) {
                Product product = productRepository.findById(line.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Product", line.getProductId()));
                product.setQuantityOnHand(product.getQuantityOnHand() + line.getQuantity());
                productRepository.save(product);
            }
            log.info("Order id={} cancelled — stock restored for {} lines", orderId, order.getLines().size());
        }

        orderRepository.updateStatus(orderId, newStatus);
        log.info("Order id={} status → {}", orderId, newStatus);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING ->
                next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING ->
                next == OrderStatus.SHIPPED || next == OrderStatus.COMPLETED || next == OrderStatus.CANCELLED;
            case SHIPPED ->
                next == OrderStatus.IN_TRANSIT || next == OrderStatus.DELIVERED
                        || next == OrderStatus.COMPLETED || next == OrderStatus.CANCELLED;
            case IN_TRANSIT ->
                next == OrderStatus.DELIVERED || next == OrderStatus.COMPLETED || next == OrderStatus.CANCELLED;
            case DELIVERED -> next == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new ServiceException("Invalid transition: " + current + " → " + next);
        }
    }
}
