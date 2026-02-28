package com.jewelry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single line item inside an {@link Order}.
 *
 * <p>Both {@code unitPrice} and {@code costPrice} are <em>snapshotted</em>
 * at order-creation time so profit calculations remain accurate even after
 * product prices change later.
 */
@Entity
@Table(name = "order_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Min(value = 1, message = "Quantity must be at least 1.")
    @Column(nullable = false)
    private int quantity;

    @NotNull(message = "Unit price is required.")
    @PositiveOrZero(message = "Unit price cannot be negative.")
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @NotNull(message = "Cost price is required.")
    @PositiveOrZero(message = "Cost price cannot be negative.")
    @Column(name = "cost_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal costPrice;

    // ── Derived ───────────────────────────────────────────────────────────────

    public BigDecimal getLineTotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getLineProfit() {
        if (unitPrice == null || costPrice == null) return BigDecimal.ZERO;
        return unitPrice.subtract(costPrice).multiply(BigDecimal.valueOf(quantity));
    }
}
