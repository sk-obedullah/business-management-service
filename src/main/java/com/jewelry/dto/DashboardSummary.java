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
public class DashboardSummary {

    // ── KPI Cards ─────────────────────────────────────────────────────────────
    private BigDecimal totalRevenue = BigDecimal.ZERO; // sum of completed order totals
    private BigDecimal totalProfit = BigDecimal.ZERO; // sum of (sell - cost) × qty for completed lines
    private BigDecimal profitMarginPct = BigDecimal.ZERO; // totalProfit / totalRevenue * 100
    private long ordersToday = 0;
    private long pendingOrders = 0;
    private long totalCustomers = 0;
    private long totalProducts = 0;
    private long lowStockCount = 0;

    // ── Monthly Revenue (last 6 months) for BarChart ──────────────────────────
    // Key: "MMM yyyy" (e.g. "Jan 2025"), Value: revenue BigDecimal
    private Map<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();

    // ── Order Status breakdown for PieChart ───────────────────────────────────
    // Key: status display name, Value: count
    private Map<String, Long> ordersByStatus = new LinkedHashMap<>();

    // ── Top 5 selling products ────────────────────────────────────────────────
    private List<TopProduct> topProducts;

    // ── Low-stock products ────────────────────────────────────────────────────
    private List<LowStockProduct> lowStockProducts;

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

    // ── Getters & Setters ────────────────────────────────────────────────────

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal v) {
        this.totalRevenue = v;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal v) {
        this.totalProfit = v;
    }

    public BigDecimal getProfitMarginPct() {
        return profitMarginPct;
    }

    public void setProfitMarginPct(BigDecimal v) {
        this.profitMarginPct = v;
    }

    public long getOrdersToday() {
        return ordersToday;
    }

    public void setOrdersToday(long v) {
        this.ordersToday = v;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long v) {
        this.pendingOrders = v;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long v) {
        this.totalCustomers = v;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long v) {
        this.totalProducts = v;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long v) {
        this.lowStockCount = v;
    }

    public Map<String, BigDecimal> getMonthlyRevenue() {
        return monthlyRevenue;
    }

    public void setMonthlyRevenue(Map<String, BigDecimal> m) {
        this.monthlyRevenue = m;
    }

    public Map<String, Long> getOrdersByStatus() {
        return ordersByStatus;
    }

    public void setOrdersByStatus(Map<String, Long> m) {
        this.ordersByStatus = m;
    }

    public List<TopProduct> getTopProducts() {
        return topProducts;
    }

    public void setTopProducts(List<TopProduct> list) {
        this.topProducts = list;
    }

    public List<LowStockProduct> getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(List<LowStockProduct> l) {
        this.lowStockProducts = l;
    }
}
