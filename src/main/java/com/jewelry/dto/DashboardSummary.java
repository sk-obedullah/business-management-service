package com.jewelry.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated summary DTO for the Dashboard.
 * Populated by a single call to
 * {@link com.jewelry.service.DashboardService#getSummary()}
 * and then consumed by the {@link com.jewelry.controller.DashboardController}.
 *
 * <p>
 * All fields are read-only from the UI's perspective — it only displays.
 */
public record DashboardSummary(
        BigDecimal totalRevenue,
        BigDecimal totalProfit,
        BigDecimal profitMarginPct,
        long ordersToday,
        long pendingOrders,
        long totalCustomers,
        long totalProducts,
        long lowStockCount,
        Map<String, BigDecimal> monthlyRevenue,
        Map<String, Long> ordersByStatus,
        List<TopProduct> topProducts,
        List<LowStockProduct> lowStockProducts
) {

    // ── Nested record types ───────────────────────────────────────────────────

    public record TopProduct(
            String name,
            String sku,
            long unitsSold,
            BigDecimal revenue) {
    }

    public record LowStockProduct(
            String name,
            String sku,
            int quantityOnHand) {
    }
}
