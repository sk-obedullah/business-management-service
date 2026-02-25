package com.jewelry.service.impl;

import com.jewelry.dto.DashboardSummary;
import com.jewelry.dto.DashboardSummary.LowStockProduct;
import com.jewelry.dto.DashboardSummary.TopProduct;
import com.jewelry.exception.ServiceException;
import com.jewelry.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;

import java.util.*;

/**
 * JDBC implementation of {@link DashboardService}.
 *
 * <p>
 * Each method fires one focused SQL query. All are read-only.
 * They are assembled into a single {@link DashboardSummary} in
 * {@link #getSummary()}.
 *
 * <p>
 * <strong>Query design notes:</strong>
 * <ul>
 * <li>Revenue/profit only counts COMPLETED orders to avoid
 * inflating figures with pending/cancelled ones.</li>
 * <li>Monthly revenue uses MySQL {@code DATE_FORMAT} to bucket
 * by "YYYY-MM"; results are re-formatted to "MMM yyyy" in Java.</li>
 * <li>Top products join {@code order_line} with {@code `order`} and
 * filter by COMPLETED status before aggregating.</li>
 * <li>Low-stock threshold is a constant (5) matching
 * {@code ProductListController}.</li>
 * </ul>
 */
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final DataSource dataSource;

    public DashboardServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DashboardSummary getSummary() {
        log.debug("Building dashboard summary...");
        DashboardSummary s = new DashboardSummary();

        try (Connection conn = dataSource.getConnection()) {
            loadRevenuAndProfit(conn, s);
            loadOrderCounts(conn, s);
            loadEntityCounts(conn, s);
            loadMonthlyRevenue(conn, s);
            loadOrdersByStatus(conn, s);
            loadTopProducts(conn, s);
            loadLowStockProducts(conn, s);
        } catch (SQLException e) {
            throw new ServiceException("Dashboard query failed", e);
        }

        // Compute margin %
        if (s.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            s.setProfitMarginPct(
                    s.getTotalProfit()
                            .divide(s.getTotalRevenue(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP));
        }

        log.debug("Dashboard summary built: revenue={} profit={}",
                s.getTotalRevenue(), s.getTotalProfit());
        return s;
    }

    // ── Individual query helpers ──────────────────────────────────────────────

    private void loadRevenuAndProfit(Connection conn, DashboardSummary s) throws SQLException {
        String sql = """
                SELECT
                  COALESCE(SUM(o.total_amount - COALESCE(o.discount,0)), 0)   AS revenue,
                  COALESCE(SUM(ol.quantity * (ol.unit_price - ol.cost_price)), 0) AS profit
                FROM `order` o
                JOIN order_line ol ON ol.order_id = o.id
                WHERE o.status = 'COMPLETED'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                s.setTotalRevenue(rs.getBigDecimal("revenue"));
                s.setTotalProfit(rs.getBigDecimal("profit"));
            }
        }
    }

    private void loadOrderCounts(Connection conn, DashboardSummary s) throws SQLException {
        // Orders created today
        String todaySql = """
                SELECT COUNT(*) FROM `order`
                WHERE DATE(created_at) = CURDATE()
                """;
        try (PreparedStatement ps = conn.prepareStatement(todaySql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                s.setOrdersToday(rs.getLong(1));
        }

        // Pending orders
        String pendingSql = "SELECT COUNT(*) FROM `order` WHERE status = 'PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(pendingSql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                s.setPendingOrders(rs.getLong(1));
        }
    }

    private void loadEntityCounts(Connection conn, DashboardSummary s) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM customer");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                s.setTotalCustomers(rs.getLong(1));
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM product");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                s.setTotalProducts(rs.getLong(1));
        }
        String lowSql = "SELECT COUNT(*) FROM product WHERE quantity_on_hand <= " + LOW_STOCK_THRESHOLD;
        try (PreparedStatement ps = conn.prepareStatement(lowSql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                s.setLowStockCount(rs.getLong(1));
        }
    }

    private void loadMonthlyRevenue(Connection conn, DashboardSummary s) throws SQLException {
        String sql = """
                SELECT
                  DATE_FORMAT(o.created_at, '%Y-%m') AS ym,
                  COALESCE(SUM(o.total_amount - COALESCE(o.discount,0)), 0) AS revenue
                FROM `order` o
                WHERE o.status = 'COMPLETED'
                  AND o.created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
                GROUP BY ym
                ORDER BY ym ASC
                """;
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String ym = rs.getString("ym"); // e.g. "2025-01"
                String label = formatYearMonth(ym); // e.g. "Jan 2025"
                monthly.put(label, rs.getBigDecimal("revenue"));
            }
        }
        s.setMonthlyRevenue(monthly);
    }

    private void loadOrdersByStatus(Connection conn, DashboardSummary s) throws SQLException {
        String sql = "SELECT status, COUNT(*) AS cnt FROM `order` GROUP BY status";
        Map<String, Long> map = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // Capitalise first letter for display
                String raw = rs.getString("status");
                String display = raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
                map.put(display, rs.getLong("cnt"));
            }
        }
        s.setOrdersByStatus(map);
    }

    private void loadTopProducts(Connection conn, DashboardSummary s) throws SQLException {
        String sql = """
                SELECT
                  p.name,
                  p.sku,
                  SUM(ol.quantity)                                      AS units_sold,
                  SUM(ol.quantity * ol.unit_price)                      AS revenue
                FROM order_line ol
                JOIN `order` o ON o.id = ol.order_id
                JOIN product  p ON p.id = ol.product_id
                WHERE o.status = 'COMPLETED'
                GROUP BY p.id, p.name, p.sku
                ORDER BY units_sold DESC
                LIMIT 5
                """;
        List<TopProduct> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TopProduct(
                        rs.getString("name"),
                        rs.getString("sku"),
                        rs.getLong("units_sold"),
                        rs.getBigDecimal("revenue")));
            }
        }
        s.setTopProducts(list);
    }

    private void loadLowStockProducts(Connection conn, DashboardSummary s) throws SQLException {
        String sql = """
                SELECT name, sku, quantity_on_hand
                FROM product
                WHERE quantity_on_hand <= ?
                ORDER BY quantity_on_hand ASC
                LIMIT 10
                """;
        List<LowStockProduct> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, LOW_STOCK_THRESHOLD);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LowStockProduct(
                            rs.getString("name"),
                            rs.getString("sku"),
                            rs.getInt("quantity_on_hand")));
                }
            }
        }
        s.setLowStockProducts(list);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Converts "2025-01" → "Jan 2025" */
    private String formatYearMonth(String ym) {
        try {
            int year = Integer.parseInt(ym.substring(0, 4));
            int month = Integer.parseInt(ym.substring(5, 7));
            return java.time.Month.of(month).getDisplayName(
                    java.time.format.TextStyle.SHORT, Locale.ENGLISH) + " " + year;
        } catch (Exception e) {
            return ym;
        }
    }
}
