package com.jewelry.service.impl;

import com.jewelry.dto.DashboardSummary;
import com.jewelry.dto.DashboardSummary.LowStockProduct;
import com.jewelry.dto.DashboardSummary.TopProduct;
import com.jewelry.service.DashboardService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * Implementation of {@link DashboardService} using JPA native queries.
 * All queries are read-only; Spring manages the transaction context.
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final int LOW_STOCK_THRESHOLD = 5;

    @PersistenceContext
    private EntityManager em;

    @Override
    public DashboardSummary getSummary() {
        log.debug("Building dashboard summary...");
        DashboardSummary s = new DashboardSummary();

        loadRevenue(s);
        loadOrderCounts(s);
        loadEntityCounts(s);
        loadMonthlyRevenue(s);
        loadOrdersByStatus(s);
        loadTopProducts(s);
        loadLowStockProducts(s);

        if (s.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            s.setProfitMarginPct(
                    s.getTotalProfit()
                            .divide(s.getTotalRevenue(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP));
        }

        log.debug("Dashboard summary built: revenue={} profit={}", s.getTotalRevenue(), s.getTotalProfit());
        return s;
    }

    // ── Individual query helpers ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadRevenue(DashboardSummary s) {
        String sql = """
                SELECT
                  COALESCE(SUM(o.total_amount - COALESCE(o.discount,0)), 0)        AS revenue,
                  COALESCE(SUM(ol.quantity * (ol.unit_price - ol.cost_price)), 0)  AS profit
                FROM "order" o
                JOIN order_line ol ON ol.order_id = o.id
                WHERE o.status = 'COMPLETED'
                """;
        Object[] row = (Object[]) em.createNativeQuery(sql).getSingleResult();
        s.setTotalRevenue(toBigDecimal(row[0]));
        s.setTotalProfit(toBigDecimal(row[1]));
    }

    private void loadOrderCounts(DashboardSummary s) {
        String todaySql = """
                SELECT COUNT(*) FROM "order"
                WHERE CAST(created_at AS DATE) = CURRENT_DATE
                """;
        s.setOrdersToday(toLong(em.createNativeQuery(todaySql).getSingleResult()));

        String pendingSql = "SELECT COUNT(*) FROM \"order\" WHERE status = 'PENDING'";
        s.setPendingOrders(toLong(em.createNativeQuery(pendingSql).getSingleResult()));
    }

    private void loadEntityCounts(DashboardSummary s) {
        s.setTotalCustomers(toLong(em.createNativeQuery("SELECT COUNT(*) FROM customer").getSingleResult()));
        s.setTotalProducts(toLong(em.createNativeQuery("SELECT COUNT(*) FROM product").getSingleResult()));
        String lowSql = "SELECT COUNT(*) FROM product WHERE quantity_on_hand <= " + LOW_STOCK_THRESHOLD;
        s.setLowStockCount(toLong(em.createNativeQuery(lowSql).getSingleResult()));
    }

    @SuppressWarnings("unchecked")
    private void loadMonthlyRevenue(DashboardSummary s) {
        String sql = """
                SELECT
                  FORMATDATETIME(o.created_at, 'yyyy-MM') AS ym,
                  COALESCE(SUM(o.total_amount - COALESCE(o.discount,0)), 0) AS revenue
                FROM "order" o
                WHERE o.status = 'COMPLETED'
                  AND o.created_at >= DATEADD('MONTH', -6, NOW())
                GROUP BY ym
                ORDER BY ym ASC
                """;
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        for (Object[] r : rows) {
            String label = formatYearMonth((String) r[0]);
            monthly.put(label, toBigDecimal(r[1]));
        }
        s.setMonthlyRevenue(monthly);
    }

    @SuppressWarnings("unchecked")
    private void loadOrdersByStatus(DashboardSummary s) {
        String sql = "SELECT status, COUNT(*) AS cnt FROM \"order\" GROUP BY status";
        Map<String, Long> map = new LinkedHashMap<>();
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        for (Object[] r : rows) {
            String raw = (String) r[0];
            String display = raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
            map.put(display, toLong(r[1]));
        }
        s.setOrdersByStatus(map);
    }

    @SuppressWarnings("unchecked")
    private void loadTopProducts(DashboardSummary s) {
        String sql = """
                SELECT p.name, p.sku,
                       SUM(ol.quantity)               AS units_sold,
                       SUM(ol.quantity * ol.unit_price) AS revenue
                FROM order_line ol
                JOIN "order"  o ON o.id = ol.order_id
                JOIN product  p ON p.id = ol.product_id
                WHERE o.status = 'COMPLETED'
                GROUP BY p.id, p.name, p.sku
                ORDER BY units_sold DESC
                LIMIT 5
                """;
        List<TopProduct> list = new ArrayList<>();
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        for (Object[] r : rows) {
            list.add(new TopProduct((String) r[0], (String) r[1], toLong(r[2]), toBigDecimal(r[3])));
        }
        s.setTopProducts(list);
    }

    @SuppressWarnings("unchecked")
    private void loadLowStockProducts(DashboardSummary s) {
        String sql = """
                SELECT name, sku, quantity_on_hand
                FROM product
                WHERE quantity_on_hand <= :threshold
                ORDER BY quantity_on_hand ASC
                LIMIT 10
                """;
        List<LowStockProduct> list = new ArrayList<>();
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("threshold", LOW_STOCK_THRESHOLD)
                .getResultList();
        for (Object[] r : rows) {
            list.add(new LowStockProduct((String) r[0], (String) r[1], ((Number) r[2]).intValue()));
        }
        s.setLowStockProducts(list);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof BigInteger bi) return bi.longValue();
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }

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
