package com.jewelry.repository.impl;

import com.jewelry.entity.Order;
import com.jewelry.entity.OrderStatus;
import com.jewelry.exception.ServiceException;
import com.jewelry.repository.OrderRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link OrderRepository}.
 *
 * <p>
 * JOIN query fetches customer name in a single round-trip.
 * Lines are NOT loaded here — the service layer calls
 * {@link OrderLineRepositoryImpl} separately.
 */
public class OrderRepositoryImpl extends AbstractJdbcRepository implements OrderRepository {

    private static final String INSERT_SQL = """
            INSERT INTO `order`
              (customer_id, order_date, status, total_amount, discount, notes, created_at, updated_at)
            VALUES (?, NOW(), ?, ?, ?, ?, NOW(), NOW())
            """;

    private static final String SELECT_ALL = """
            SELECT o.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
                   c.phone AS customer_phone, c.email AS customer_email, c.address AS customer_address
            FROM `order` o
            LEFT JOIN customer c ON c.id = o.customer_id
            ORDER BY o.created_at DESC
            """;

    private static final String SELECT_BY_ID = """
            SELECT o.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
                   c.phone AS customer_phone, c.email AS customer_email, c.address AS customer_address
            FROM `order` o
            LEFT JOIN customer c ON c.id = o.customer_id
            WHERE o.id = ?
            """;

    private static final String SELECT_BY_CUSTOMER = """
            SELECT o.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
                   c.phone AS customer_phone, c.email AS customer_email, c.address AS customer_address
            FROM `order` o
            LEFT JOIN customer c ON c.id = o.customer_id
            WHERE o.customer_id = ?
            ORDER BY o.created_at DESC
            """;

    private static final String SELECT_BY_STATUS = """
            SELECT o.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
                   c.phone AS customer_phone, c.email AS customer_email, c.address AS customer_address
            FROM `order` o
            LEFT JOIN customer c ON c.id = o.customer_id
            WHERE o.status = ?
            ORDER BY o.created_at DESC
            """;

    private static final String UPDATE_SQL = """
            UPDATE `order` SET
              customer_id = ?, status = ?, total_amount = ?,
              discount = ?, notes = ?, updated_at = NOW()
            WHERE id = ?
            """;

    private static final String UPDATE_STATUS = "UPDATE `order` SET status = ?, updated_at = NOW() WHERE id = ?";
    private static final String UPDATE_TOTAL = "UPDATE `order` SET total_amount = ?, updated_at = NOW() WHERE id = ?";
    private static final String DELETE_BY_ID = "DELETE FROM `order` WHERE id = ?";

    public OrderRepositoryImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Order save(Order order) {
        long id = executeInsert(INSERT_SQL,
                order.getCustomerId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getDiscount(),
                order.getNotes());
        order.setId(id);
        log.debug("Saved order id={}", id);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ServiceException("findById order failed id=" + id, e);
        }
    }

    @Override
    public List<Order> findAll() {
        List<Order> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new ServiceException("findAll orders failed", e);
        }
        return list;
    }

    @Override
    public void update(Order order) {
        executeUpdate(UPDATE_SQL,
                order.getCustomerId(), order.getStatus().name(),
                order.getTotalAmount(), order.getDiscount(),
                order.getNotes(), order.getId());
    }

    @Override
    public void deleteById(Long id) {
        executeUpdate(DELETE_BY_ID, id);
    }

    @Override
    public List<Order> findByCustomerId(Long customerId) {
        List<Order> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_CUSTOMER)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new ServiceException("findByCustomerId failed", e);
        }
        return list;
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        List<Order> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_STATUS)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new ServiceException("findByStatus failed", e);
        }
        return list;
    }

    @Override
    public void updateStatus(Long orderId, OrderStatus newStatus) {
        executeUpdate(UPDATE_STATUS, newStatus.name(), orderId);
        log.debug("Updated order id={} status={}", orderId, newStatus);
    }

    @Override
    public void updateTotal(Long orderId, BigDecimal totalAmount) {
        executeUpdate(UPDATE_TOTAL, totalAmount, orderId);
    }

    // ── Row Mapper ───────────────────────────────────────────────────────────

    private Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setCustomerId(rs.getLong("customer_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setCustomerPhone(rs.getString("customer_phone"));
        o.setCustomerEmail(rs.getString("customer_email"));
        o.setCustomerAddress(rs.getString("customer_address"));
        o.setStatus(OrderStatus.fromString(rs.getString("status")));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setDiscount(rs.getBigDecimal("discount"));
        o.setNotes(rs.getString("notes"));
        Timestamp orderDate = rs.getTimestamp("order_date");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (orderDate != null)
            o.setOrderDate(orderDate.toLocalDateTime());
        if (createdAt != null)
            o.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null)
            o.setUpdatedAt(updatedAt.toLocalDateTime());
        return o;
    }
}
