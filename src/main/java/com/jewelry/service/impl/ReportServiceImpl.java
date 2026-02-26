package com.jewelry.service.impl;

import com.jewelry.exception.ServiceException;
import com.jewelry.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link ReportService}.
 *
 * <p>
 * All report data is pulled via dedicated SQL queries and serialised to
 * RFC-4180–compliant CSV using a minimal zero-dependency writer built in-house:
 * values containing commas, quotes, or newlines are double-quoted and internal
 * quotes are escaped as {@code ""}.
 *
 * <p>
 * No Apache Commons CSV or other library is required — this keeps the
 * project free of extra transitive dependencies at this stage.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> inject via constructor,
 * expose as a streaming endpoint, and replace {@link File} with
 * {@code OutputStream}.
 */
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final DataSource dataSource;

    public ReportServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int exportCsv(ReportType type, File targetFile,
            LocalDate from, LocalDate to,
            String statusFilter) {
        log.info("Exporting {} to {}", type, targetFile.getAbsolutePath());
        try (PrintWriter writer = new PrintWriter(
                new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8)))) {

            int rows = switch (type) {
                case ORDER_REPORT -> exportOrders(writer, from, to, statusFilter);
                case REVENUE_SUMMARY -> exportRevenueSummary(writer, from, to);
                case INVENTORY_REPORT -> exportInventory(writer);
                case CUSTOMER_HISTORY -> exportCustomerHistory(writer, from, to);
            };

            log.info("Exported {} rows for {}", rows, type);
            return rows;

        } catch (IOException e) {
            throw new ServiceException("Failed to write CSV file: " + e.getMessage(), e);
        }
    }

    // ── Order Report ─────────────────────────────────────────────────────────

    private int exportOrders(PrintWriter w, LocalDate from, LocalDate to, String statusFilter)
            throws IOException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                  o.id,
                  CONCAT(c.first_name, ' ', c.last_name) AS customer,
                  c.email,
                  FORMATDATETIME(o.order_date, 'yyyy-MM-dd') AS order_date,
                  o.status,
                  o.total_amount,
                  COALESCE(o.discount, 0)                 AS discount,
                  (o.total_amount - COALESCE(o.discount,0)) AS net_amount,
                  COALESCE(SUM(ol.quantity*(ol.unit_price - ol.cost_price)), 0) AS profit,
                  COUNT(ol.id)                            AS item_count,
                  o.notes
                FROM "order" o
                LEFT JOIN customer   c  ON c.id = o.customer_id
                LEFT JOIN order_line ol ON ol.order_id = o.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        if (from != null) {
            sql.append(" AND o.order_date >= ? ");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND o.order_date <= ? ");
            params.add(Date.valueOf(to));
        }
        if (statusFilter != null && !statusFilter.isBlank() && !"All".equals(statusFilter)) {
            sql.append(" AND o.status = ? ");
            params.add(statusFilter.toUpperCase());
        }
        sql.append(
                " GROUP BY o.id, c.first_name, c.last_name, c.email, o.order_date, o.status, o.total_amount, o.discount, o.notes ORDER BY o.id ASC");

        writeRow(w, "Order ID", "Customer", "Email", "Order Date", "Status",
                "Total (₹)", "Discount (₹)", "Net (₹)", "Profit (₹)", "Items", "Notes");

        return executeQuery(sql.toString(), params, w, rs -> {
            writeRow(w,
                    rs.getString("id"),
                    rs.getString("customer"),
                    rs.getString("email"),
                    rs.getString("order_date"),
                    rs.getString("status"),
                    rs.getString("total_amount"),
                    rs.getString("discount"),
                    rs.getString("net_amount"),
                    rs.getString("profit"),
                    rs.getString("item_count"),
                    rs.getString("notes"));
        });
    }

    // ── Revenue Summary ──────────────────────────────────────────────────────

    private int exportRevenueSummary(PrintWriter w, LocalDate from, LocalDate to)
            throws IOException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                  FORMATDATETIME(o.order_date, 'yyyy-MM')                       AS month,
                  COUNT(DISTINCT o.id)                                         AS order_count,
                  COALESCE(SUM(o.total_amount - COALESCE(o.discount,0)), 0)    AS revenue,
                  COALESCE(SUM(ol.quantity*(ol.unit_price - ol.cost_price)),0) AS profit
                FROM "order" o
                LEFT JOIN order_line ol ON ol.order_id = o.id
                WHERE o.status = 'COMPLETED'
                """);
        List<Object> params = new ArrayList<>();
        if (from != null) {
            sql.append(" AND o.order_date >= ? ");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND o.order_date <= ? ");
            params.add(Date.valueOf(to));
        }
        sql.append(" GROUP BY month ORDER BY month ASC");

        writeRow(w, "Month", "Orders", "Revenue (₹)", "Profit (₹)", "Margin %");

        return executeQuery(sql.toString(), params, w, rs -> {
            double revenue = rs.getDouble("revenue");
            double profit = rs.getDouble("profit");
            String margin = revenue > 0 ? String.format("%.1f", profit / revenue * 100) : "0.0";
            writeRow(w,
                    rs.getString("month"),
                    rs.getString("order_count"),
                    String.valueOf(revenue),
                    String.valueOf(profit),
                    margin);
        });
    }

    // ── Inventory Report ─────────────────────────────────────────────────────

    private int exportInventory(PrintWriter w) throws IOException {
        String sql = """
                SELECT
                  name, sku, category, metal,
                  cost_price, selling_price,
                  (selling_price - cost_price)   AS unit_profit,
                  ROUND((selling_price - cost_price) / selling_price * 100, 1) AS margin_pct,
                  quantity_on_hand,
                  (cost_price * quantity_on_hand) AS stock_value
                FROM product
                ORDER BY name ASC
                """;
        writeRow(w, "Product", "SKU", "Category", "Material",
                "Cost (₹)", "Price (₹)", "Unit Profit (₹)", "Margin %", "Stock", "Stock Value (₹)");
        return executeQuery(sql, List.of(), w, rs -> {
            writeRow(w,
                    rs.getString("name"),
                    rs.getString("sku"),
                    rs.getString("category"),
                    rs.getString("metal"),
                    rs.getString("cost_price"),
                    rs.getString("selling_price"),
                    rs.getString("unit_profit"),
                    rs.getString("margin_pct"),
                    rs.getString("quantity_on_hand"),
                    rs.getString("stock_value"));
        });
    }

    // ── Customer Purchase History ─────────────────────────────────────────────

    private int exportCustomerHistory(PrintWriter w, LocalDate from, LocalDate to)
            throws IOException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                  CONCAT(c.first_name, ' ', c.last_name) AS customer,
                  c.email,
                  c.phone,
                  COUNT(DISTINCT o.id)                   AS total_orders,
                  COALESCE(SUM(CASE WHEN o.status='COMPLETED'
                    THEN (o.total_amount - COALESCE(o.discount,0)) ELSE 0 END), 0) AS total_spend,
                  MAX(FORMATDATETIME(o.order_date,'yyyy-MM-dd'))                    AS last_order_date
                FROM customer c
                LEFT JOIN "order" o ON o.customer_id = c.id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (from != null) {
            sql.append(" AND (o.order_date IS NULL OR o.order_date >= ?) ");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND (o.order_date IS NULL OR o.order_date <= ?) ");
            params.add(Date.valueOf(to));
        }
        sql.append(" GROUP BY c.id, c.first_name, c.last_name, c.email, c.phone ORDER BY total_spend DESC");

        writeRow(w, "Customer", "Email", "Phone", "Total Orders", "Total Spend (₹)", "Last Order Date");
        return executeQuery(sql.toString(), params, w, rs -> {
            writeRow(w,
                    rs.getString("customer"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("total_orders"),
                    rs.getString("total_spend"),
                    rs.getString("last_order_date"));
        });
    }

    // ── CSV helpers ──────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface RowMapper {
        void map(ResultSet rs) throws SQLException, IOException;
    }

    private int executeQuery(String sql, List<Object> params, PrintWriter w, RowMapper mapper)
            throws IOException {
        int count = 0;
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapper.map(rs);
                    count++;
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Report query failed: " + e.getMessage(), e);
        }
        return count;
    }

    /**
     * Writes a single CSV row, RFC-4180 compliant.
     * Values with commas, double-quotes, or newlines are wrapped in double-quotes;
     * embedded double-quotes are escaped as {@code ""}.
     */
    private void writeRow(PrintWriter w, String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0)
                sb.append(',');
            String val = fields[i] != null ? fields[i] : "";
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        w.println(sb);
    }
}
