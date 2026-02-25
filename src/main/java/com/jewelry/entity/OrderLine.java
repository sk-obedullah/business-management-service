package com.jewelry.entity;

import java.math.BigDecimal;

/**
 * A single line item inside an {@link Order}.
 *
 * <p>
 * Both {@code unitPrice} and {@code costPrice} are <em>snapshotted</em>
 * at the moment the order is created. This ensures profit calculations remain
 * accurate even after the product's price is later updated.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> add
 * {@code @Entity}, {@code @ManyToOne @JoinColumn(name="order_id")} for
 * {@code order},
 * and {@code @ManyToOne @JoinColumn(name="product_id")} for {@code product}.
 */
public class OrderLine {

    private Long id;
    private Long orderId;
    private Long productId;
    private String productName; // denormalised for display
    private String productSku; // denormalised for display
    private int quantity;
    private BigDecimal unitPrice; // snapshot of selling_price at order time
    private BigDecimal costPrice; // snapshot of cost_price at order time

    // ── Constructors ─────────────────────────────────────────────────────────
    public OrderLine() {
    }

    public OrderLine(Long productId, String productName, String productSku,
            int quantity, BigDecimal unitPrice, BigDecimal costPrice) {
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.costPrice = costPrice;
    }

    // ── Derived ──────────────────────────────────────────────────────────────

    /** Line total = unitPrice × quantity. */
    public BigDecimal getLineTotal() {
        if (unitPrice == null)
            return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** Line profit = (unitPrice − costPrice) × quantity. */
    public BigDecimal getLineProfit() {
        if (unitPrice == null || costPrice == null)
            return BigDecimal.ZERO;
        return unitPrice.subtract(costPrice).multiply(BigDecimal.valueOf(quantity));
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

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
}
