package com.jewelry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a customer order.
 *
 * <p>An Order owns a collection of {@link OrderLine} items loaded eagerly.
 * {@code totalAmount} is stored as a pre-computed cache for fast reporting.
 */
@Entity
@Table(name = "\"order\"")   // "order" is a reserved SQL keyword — must be quoted
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Customer is required.")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    /** Denormalised for display convenience — fetched via JOIN in service layer. */
    @Transient
    private String customerName;
    @Transient
    private String customerPhone;
    @Transient
    private String customerEmail;
    @Transient
    private String customerAddress;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @NotNull(message = "Total amount is required.")
    @PositiveOrZero(message = "Total amount cannot be negative.")
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal discount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderLine> lines = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) orderDate = LocalDateTime.now();
        if (status == null) status = OrderStatus.PENDING;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PostLoad
    protected void onLoad() {
        if (customer != null) {
            this.customerName = customer.getFirstName() + " " + customer.getLastName();
            this.customerPhone = customer.getPhone();
            this.customerEmail = customer.getEmail();
            this.customerAddress = customer.getAddress();
        }
    }

    // ── Derived ───────────────────────────────────────────────────────────────

    public BigDecimal getNetAmount() {
        BigDecimal d = (discount != null) ? discount : BigDecimal.ZERO;
        BigDecimal t = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
        return t.subtract(d);
    }

    public BigDecimal getTotalCost() {
        return lines.stream()
                .map(l -> l.getCostPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getGrossProfit() {
        return getNetAmount().subtract(getTotalCost());
    }
}
