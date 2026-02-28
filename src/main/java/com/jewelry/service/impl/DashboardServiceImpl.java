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
        
        var revProf = loadRevenue();
        var orderCounts = loadOrderCounts();
        var entityCounts = loadEntityCounts();
        
        var totalRevenue = revProf[0];
        var totalProfit = revProf[1];

        var profitMarginPct = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            profitMarginPct = totalProfit
                            .divide(totalRevenue, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP);
        }

        var s = new DashboardSummary(
                totalRevenue,
                totalProfit,
                profitMarginPct,
                orderCounts[0],
                orderCounts[1],
                entityCounts[0],
                entityCounts[1],
                entityCounts[2],
                loadMonthlyRevenue(),
                loadOrdersByStatus(),
                loadTopProducts(),
                loadLowStockProducts()
        );

        log.debug("Dashboard summary built: revenue={} profit={}", s.totalRevenue(), s.totalProfit());
        return s;
    }

    // ── Individual query helpers ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private BigDecimal[] loadRevenue() {
        String sql = """
                SELECT
                  COALESCE(SUM(o.total_amount - COALESCE(o.discount,0)), 0)        AS revenue,
                  COALESCE(SUM(ol.quantity * (ol.unit_price - ol.cost_price)), 0)  AS profit
                FROM "order" o
                JOIN order_line ol ON ol.order_id = o.id
                WHERE o.status = 'COMPLETED'
                """;
        var row = (Object[]) em.createNativeQuery(sql).getSingleResult();
        return new BigDecimal[] { toBigDecimal(row[0]), toBigDecimal(row[1]) };
    }

    private long[] loadOrderCounts() {
        String todaySql = """
                SELECT COUNT(*) FROM "order"
                WHERE CAST(created_at AS DATE) = CURRENT_DATE
                """;
        var today = toLong(em.createNativeQuery(todaySql).getSingleResult());

        var pendingSql = "SELECT COUNT(*) FROM \"order\" WHERE status = 'PENDING'";
        var pending = toLong(em.createNativeQuery(pendingSql).getSingleResult());
        
        return new long[] { today, pending };
    }

    private long[] loadEntityCounts() {
        var cust = toLong(em.createNativeQuery("SELECT COUNT(*) FROM customer").getSingleResult());
        var prod = toLong(em.createNativeQuery("SELECT COUNT(*) FROM product").getSingleResult());
        var lowSql = "SELECT COUNT(*) FROM product WHERE quantity_on_hand <= " + LOW_STOCK_THRESHOLD;
        var low = toLong(em.createNativeQuery(lowSql).getSingleResult());
        
        return new long[] { cust, prod, low };
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> loadMonthlyRevenue() {
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
        var rows = (List<Object[]>) em.createNativeQuery(sql).getResultList();
        rows.forEach(r -> {
            var label = formatYearMonth((String) r[0]);
            monthly.put(label, toBigDecimal(r[1]));
        });
        return monthly;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> loadOrdersByStatus() {
        String sql = "SELECT status, COUNT(*) AS cnt FROM \"order\" GROUP BY status";
        Map<String, Long> map = new LinkedHashMap<>();
        var rows = (List<Object[]>) em.createNativeQuery(sql).getResultList();
        rows.forEach(r -> {
            var raw = (String) r[0];
            var display = raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
            map.put(display, toLong(r[1]));
        });
        return map;
    }

    @SuppressWarnings("unchecked")
    private List<TopProduct> loadTopProducts() {
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
        var rows = (List<Object[]>) em.createNativeQuery(sql).getResultList();
        rows.forEach(r -> list.add(new TopProduct((String) r[0], (String) r[1], toLong(r[2]), toBigDecimal(r[3]))));
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<LowStockProduct> loadLowStockProducts() {
        String sql = """
                SELECT name, sku, quantity_on_hand
                FROM product
                WHERE quantity_on_hand <= :threshold
                ORDER BY quantity_on_hand ASC
                LIMIT 10
                """;
        List<LowStockProduct> list = new ArrayList<>();
        var rows = (List<Object[]>) em.createNativeQuery(sql)
                .setParameter("threshold", LOW_STOCK_THRESHOLD)
                .getResultList();
        rows.forEach(r -> list.add(new LowStockProduct((String) r[0], (String) r[1], ((Number) r[2]).intValue())));
        return list;
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
