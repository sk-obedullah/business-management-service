package com.jewelry.repository.impl;

import com.jewelry.entity.OrderLine;
import com.jewelry.exception.ServiceException;
import com.jewelry.repository.OrderLineRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link OrderLineRepository}.
 *
 * <p>
 * Uses a JOIN to fetch product name and SKU in a single query,
 * avoiding a secondary select per line.
 */
public class OrderLineRepositoryImpl extends AbstractJdbcRepository implements OrderLineRepository {

    private static final String INSERT_SQL = """
            INSERT INTO order_line (order_id, product_id, quantity, unit_price, cost_price)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ORDER = """
            SELECT ol.*, p.name AS product_name, p.sku AS product_sku
            FROM order_line ol
            JOIN product p ON p.id = ol.product_id
            WHERE ol.order_id = ?
            ORDER BY ol.id ASC
            """;

    private static final String DELETE_BY_ORDER = "DELETE FROM order_line WHERE order_id = ?";

    public OrderLineRepositoryImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public OrderLine save(OrderLine line) {
        long id = executeInsert(INSERT_SQL,
                line.getOrderId(), line.getProductId(),
                line.getQuantity(), line.getUnitPrice(), line.getCostPrice());
        line.setId(id);
        return line;
    }

    @Override
    public List<OrderLine> findByOrderId(Long orderId) {
        List<OrderLine> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_ORDER)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new ServiceException("findByOrderId lines failed for orderId=" + orderId, e);
        }
        return list;
    }

    @Override
    public void deleteByOrderId(Long orderId) {
        executeUpdate(DELETE_BY_ORDER, orderId);
    }

    private OrderLine mapRow(ResultSet rs) throws SQLException {
        OrderLine ol = new OrderLine();
        ol.setId(rs.getLong("id"));
        ol.setOrderId(rs.getLong("order_id"));
        ol.setProductId(rs.getLong("product_id"));
        ol.setProductName(rs.getString("product_name"));
        ol.setProductSku(rs.getString("product_sku"));
        ol.setQuantity(rs.getInt("quantity"));
        ol.setUnitPrice(rs.getBigDecimal("unit_price"));
        ol.setCostPrice(rs.getBigDecimal("cost_price"));
        return ol;
    }
}
