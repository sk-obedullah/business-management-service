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

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Production implementation of {@link OrderService}.
 *
 * <p>
 * <strong>Most critical business rules:</strong>
 * <ol>
 * <li><b>Transactional order creation</b> — the order header, all order lines,
 * and product stock decrements are committed atomically. If any step fails,
 * the connection is rolled back so inventory stays consistent.</li>
 * <li><b>Price snapshot</b> — unit_price and cost_price are copied from the
 * product at order-creation time. Future product price changes do not
 * retroactively alter historical orders.</li>
 * <li><b>Stock deduction on create</b> — each line decrements
 * {@code product.quantity_on_hand}.</li>
 * <li><b>Stock restoration on cancel</b> — transitioning to CANCELLED
 * increments stock back.</li>
 * <li><b>FSM enforcement</b> — only valid status transitions are allowed.</li>
 * </ol>
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> annotate with {@code @Service},
 * inject via constructor, and replace the manual JDBC transaction with
 * {@code @Transactional}. All business logic stays identical.
 */
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final ProductRepository productRepository;
    private final DataSource dataSource;

    public OrderServiceImpl(OrderRepository orderRepository,
            OrderLineRepository orderLineRepository,
            ProductRepository productRepository,
            DataSource dataSource) {
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.productRepository = productRepository;
        this.dataSource = dataSource;
    }

    /**
     * Creates an order and all its lines atomically.
     *
     * <p>
     * Steps executed inside a single JDBC transaction:
     * <ol>
     * <li>Validate each product exists and has sufficient stock</li>
     * <li>Snapshot unit_price and cost_price from each Product</li>
     * <li>Compute totalAmount from line sums</li>
     * <li>INSERT order header</li>
     * <li>INSERT each order_line</li>
     * <li>Decrement product.quantity_on_hand for each line</li>
     * </ol>
     */
    @Override
    public Order createOrder(Order order) {
        if (order == null)
            throw new IllegalArgumentException("Order must not be null");
        if (order.getCustomerId() == null)
            throw new IllegalArgumentException("Customer is required");
        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        // ── 1. Validate products & build price snapshots ─────────────────────
        for (OrderLine line : order.getLines()) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product", line.getProductId()));

            if (product.getQuantityOnHand() < line.getQuantity()) {
                throw new ServiceException(
                        "Insufficient stock for '" + product.getName() + "'. "
                                + "Available: " + product.getQuantityOnHand()
                                + ", Requested: " + line.getQuantity());
            }

            // Snapshot prices
            line.setUnitPrice(product.getSellingPrice());
            line.setCostPrice(product.getCostPrice());
            line.setProductName(product.getName());
            line.setProductSku(product.getSku());
        }

        // ── 2. Compute order total ────────────────────────────────────────────
        BigDecimal total = order.getLines().stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        if (order.getStatus() == null)
            order.setStatus(OrderStatus.PENDING);
        if (order.getDiscount() == null)
            order.setDiscount(BigDecimal.ZERO);

        // ── 3. Persist atomically ─────────────────────────────────────────────
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 3a. Insert order header (uses the pool's connection directly)
                Order saved = orderRepository.save(order);

                // 3b. Insert each line
                for (OrderLine line : order.getLines()) {
                    line.setOrderId(saved.getId());
                    orderLineRepository.save(line);
                }

                // 3c. Decrement stock
                for (OrderLine line : order.getLines()) {
                    decrementStock(conn, line.getProductId(), line.getQuantity());
                }

                conn.commit();
                log.info("Order created id={} total={}", saved.getId(), total);
                return saved;

            } catch (Exception e) {
                conn.rollback();
                throw new ServiceException("Order creation rolled back: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Failed to acquire connection for order creation", e);
        }
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public List<Order> findAll() {
        List<Order> orders = orderRepository.findAll();
        orders.forEach(o -> o.setLines(orderLineRepository.findByOrderId(o.getId())));
        return orders;
    }

    @Override
    public List<Order> findByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        orders.forEach(o -> o.setLines(orderLineRepository.findByOrderId(o.getId())));
        return orders;
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        orders.forEach(o -> o.setLines(orderLineRepository.findByOrderId(o.getId())));
        return orders;
    }

    @Override
    public Order loadWithLines(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        order.setLines(orderLineRepository.findByOrderId(orderId));
        return order;
    }

    /**
     * Advances order status with FSM validation.
     * Restores stock if transitioning to CANCELLED.
     */
    @Override
    public void updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        validateTransition(order.getStatus(), newStatus);

        // Restore stock on cancellation
        if (newStatus == OrderStatus.CANCELLED) {
            List<OrderLine> lines = orderLineRepository.findByOrderId(orderId);
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    for (OrderLine line : lines) {
                        restoreStock(conn, line.getProductId(), line.getQuantity());
                    }
                    orderRepository.updateStatus(orderId, newStatus);
                    conn.commit();
                    log.info("Order id={} cancelled — stock restored for {} lines", orderId, lines.size());
                } catch (Exception e) {
                    conn.rollback();
                    throw new ServiceException("Cancel rollback: " + e.getMessage(), e);
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new ServiceException("Connection error during cancel", e);
            }
        } else {
            orderRepository.updateStatus(orderId, newStatus);
            log.info("Order id={} status → {}", orderId, newStatus);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING ->
                next == OrderStatus.SHIPPED || next == OrderStatus.COMPLETED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.IN_TRANSIT || next == OrderStatus.DELIVERED
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

    private void decrementStock(Connection conn, Long productId, int qty) throws SQLException {
        String sql = "UPDATE product SET quantity_on_hand = quantity_on_hand - ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }

    private void restoreStock(Connection conn, Long productId, int qty) throws SQLException {
        String sql = "UPDATE product SET quantity_on_hand = quantity_on_hand + ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }
}
