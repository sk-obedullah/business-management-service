package com.jewelry.dto;

import java.math.BigDecimal;

/**
 * DTO for a single order line item used in the order form table.
 */
public class OrderLineDTO {

    private Long productId;
    private String productName;
    private String productSku;
    private int quantity;
    private BigDecimal unitPrice; // selling_price snapshot
    private BigDecimal costPrice; // cost_price snapshot
    private int stockAvailable; // transient — for UI validation only

    // ── Derived ──────────────────────────────────────────────────────────────

    public BigDecimal getLineTotal() {
        if (unitPrice == null)
            return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String name) {
        this.productName = name;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String sku) {
        this.productSku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal p) {
        this.unitPrice = p;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal c) {
        this.costPrice = c;
    }

    public int getStockAvailable() {
        return stockAvailable;
    }

    public void setStockAvailable(int s) {
        this.stockAvailable = s;
    }
}
