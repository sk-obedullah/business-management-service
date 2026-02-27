package com.jewelry.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * A single line item inside an {@link Order}.
 *
 * <p>Both {@code unitPrice} and {@code costPrice} are <em>snapshotted</em>
 * at order-creation time so profit calculations remain accurate even after
 * product prices change later.
 */
@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Convenience field — derived from {@link #product} but kept for legacy compat. */
    @Transient
    private Long productId;
    @Transient
    private String productName;
    @Transient
    private String productSku;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "cost_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal costPrice;

    // ── Constructors ──────────────────────────────────────────────────────────

    public OrderLine() {}

    public OrderLine(Long productId, String productName, String productSku,
            int quantity, BigDecimal unitPrice, BigDecimal costPrice) {
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.costPrice = costPrice;
    }

    // ── Derived ───────────────────────────────────────────────────────────────

    public BigDecimal getLineTotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getLineProfit() {
        if (unitPrice == null || costPrice == null) return BigDecimal.ZERO;
        return unitPrice.subtract(costPrice).multiply(BigDecimal.valueOf(quantity));
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
            this.productName = product.getName();
            this.productSku = product.getSku();
        }
    }

    public Long getOrderId() { return order != null ? order.getId() : productId; }
    public void setOrderId(Long orderId) { /* managed via setOrder() */ }

    public Long getProductId() { return product != null ? product.getId() : productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return product != null ? product.getName() : productName; }
    public void setProductName(String name) { this.productName = name; }

    public String getProductSku() { return product != null ? product.getSku() : productSku; }
    public void setProductSku(String sku) { this.productSku = sku; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal p) { this.unitPrice = p; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal c) { this.costPrice = c; }
}
