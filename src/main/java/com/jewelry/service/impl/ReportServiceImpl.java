package com.jewelry.service.impl;

import com.jewelry.exception.ServiceException;
import com.jewelry.service.ReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link ReportService} using JPA native queries.
 * All previously raw JDBC Connection/PreparedStatement/ResultSet code is
 * replaced with EntityManager.createNativeQuery() — much cleaner, and
 * the transaction is managed by Spring @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    @PersistenceContext
    private EntityManager em;

    @Override
    public int exportCsv(ReportType type, File targetFile,
            LocalDate from, LocalDate to,
            String statusFilter) {
        log.info("Exporting {} to {}", type, targetFile.getAbsolutePath());
        try (PrintWriter writer = new PrintWriter(
                new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8)))) {

            int rows = switch (type) {
                case ORDER_REPORT    -> exportOrders(writer, from, to, statusFilter);
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

    @SuppressWarnings("unchecked")
    private int exportOrders(PrintWriter w, LocalDate from, LocalDate to, String statusFilter) {
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
        if (from != null) { sql.append(" AND o.order_date >= ? "); params.add(Date.valueOf(from)); }
        if (to   != null) { sql.append(" AND o.order_date <= ? "); params.add(Date.valueOf(to)); }
        if (statusFilter != null && !statusFilter.isBlank() && !"All".equals(statusFilter)) {
            sql.append(" AND o.status = ? ");
            params.add(statusFilter.toUpperCase());
        }
        sql.append(" GROUP BY o.id, c.first_name, c.last_name, c.email, o.order_date, o.status, o.total_amount, o.discount, o.notes ORDER BY o.id ASC");

        writeRow(w, "Order ID", "Customer", "Email", "Order Date", "Status",
                "Total (₹)", "Discount (₹)", "Net (₹)", "Profit (₹)", "Items", "Notes");

        return executeQuery(sql.toString(), params, rows -> {
            for (Object[] r : rows) {
                writeRow(w, str(r[0]), str(r[1]), str(r[2]), str(r[3]), str(r[4]),
                        str(r[5]), str(r[6]), str(r[7]), str(r[8]), str(r[9]), str(r[10]));
            }
        });
    }

    // ── Revenue Summary ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private int exportRevenueSummary(PrintWriter w, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                  FORMATDATETIME(o.order_date, 'yyyy-MM')                       AS month,
                  COUNT(DISTINCT o.id)                                           AS order_count,
                  COALESCE(SUM(o.total_amount - COALESCE(o.discount,0)), 0)     AS revenue,
                  COALESCE(SUM(ol.quantity*(ol.unit_price - ol.cost_price)), 0) AS profit
                FROM "order" o
                LEFT JOIN order_line ol ON ol.order_id = o.id
                WHERE o.status = 'COMPLETED'
                """);
        List<Object> params = new ArrayList<>();
        if (from != null) { sql.append(" AND o.order_date >= ? "); params.add(Date.valueOf(from)); }
        if (to   != null) { sql.append(" AND o.order_date <= ? "); params.add(Date.valueOf(to)); }
        sql.append(" GROUP BY month ORDER BY month ASC");

        writeRow(w, "Month", "Orders", "Revenue (₹)", "Profit (₹)", "Margin %");

        return executeQuery(sql.toString(), params, rows -> {
            for (Object[] r : rows) {
                double revenue = toDouble(r[2]);
                double profit  = toDouble(r[3]);
                String margin  = revenue > 0 ? String.format("%.1f", profit / revenue * 100) : "0.0";
                writeRow(w, str(r[0]), str(r[1]), String.valueOf(revenue), String.valueOf(profit), margin);
            }
        });
    }

    // ── Inventory Report ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private int exportInventory(PrintWriter w) {
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
        return executeQuery(sql, List.of(), rows -> {
            for (Object[] r : rows) {
                writeRow(w, str(r[0]), str(r[1]), str(r[2]), str(r[3]),
                        str(r[4]), str(r[5]), str(r[6]), str(r[7]), str(r[8]), str(r[9]));
            }
        });
    }

    // ── Customer History ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private int exportCustomerHistory(PrintWriter w, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                  CONCAT(c.first_name, ' ', c.last_name) AS customer,
                  c.email, c.phone,
                  COUNT(DISTINCT o.id)                   AS total_orders,
                  COALESCE(SUM(CASE WHEN o.status='COMPLETED'
                    THEN (o.total_amount - COALESCE(o.discount,0)) ELSE 0 END), 0) AS total_spend,
                  MAX(FORMATDATETIME(o.order_date,'yyyy-MM-dd'))                    AS last_order_date
                FROM customer c
                LEFT JOIN "order" o ON o.customer_id = c.id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (from != null) { sql.append(" AND (o.order_date IS NULL OR o.order_date >= ?) "); params.add(Date.valueOf(from)); }
        if (to   != null) { sql.append(" AND (o.order_date IS NULL OR o.order_date <= ?) "); params.add(Date.valueOf(to)); }
        sql.append(" GROUP BY c.id, c.first_name, c.last_name, c.email, c.phone ORDER BY total_spend DESC");

        writeRow(w, "Customer", "Email", "Phone", "Total Orders", "Total Spend (₹)", "Last Order Date");
        return executeQuery(sql.toString(), params, rows -> {
            for (Object[] r : rows) {
                writeRow(w, str(r[0]), str(r[1]), str(r[2]), str(r[3]), str(r[4]), str(r[5]));
            }
        });
    }

    // ── Execution helpers ─────────────────────────────────────────────────────

    @FunctionalInterface
    private interface RowsMapper {
        void map(List<Object[]> rows);
    }

    @SuppressWarnings("unchecked")
    private int executeQuery(String sql, List<Object> params, RowsMapper mapper) {
        try {
            var query = em.createNativeQuery(sql);
            for (int i = 0; i < params.size(); i++) {
                query.setParameter(i + 1, params.get(i));
            }
            List<Object[]> rows = query.getResultList();
            mapper.map(rows);
            return rows.size();
        } catch (Exception e) {
            throw new ServiceException("Report query failed: " + e.getMessage(), e);
        }
    }

    // ── CSV helpers ───────────────────────────────────────────────────────────

    private void writeRow(PrintWriter w, String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            String val = fields[i] != null ? fields[i] : "";
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        w.println(sb);
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }
}
