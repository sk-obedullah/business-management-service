package com.jewelry.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer order.
 *
 * <p>
 * An Order owns a collection of {@link OrderLine} items.
 * {@code totalAmount} is derived from line-item sums; it is stored
 * in the DB as a pre-computed cache so reports don't need to re-sum.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong>
 * Add {@code @Entity @Table(name="`order`")},
 * {@code @OneToMany(mappedBy="order",
 * cascade=ALL, orphanRemoval=true)} on {@code lines}, and
 * {@code @Enumerated(STRING)}
 * on {@code status}.
 */
public class Order {

    private Long id;
    private Long customerId;
    private String customerName; // denormalised for display convenience
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Eagerly loaded lines — always populated when fetching a single order. */
    private List<OrderLine> lines = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────────────────
    public Order() {
    }

    // ── Derived ──────────────────────────────────────────────────────────────

    /** Net payable = totalAmount − discount. */
    public BigDecimal getNetAmount() {
        BigDecimal d = (discount != null) ? discount : BigDecimal.ZERO;
        BigDecimal t = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
        return t.subtract(d);
    }

    /** Total cost of all lines (for profit calculation). */
    public BigDecimal getTotalCost() {
        return lines.stream()
                .map(l -> l.getCostPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Gross profit = net amount − total cost. */
    public BigDecimal getGrossProfit() {
        return getNetAmount().subtract(getTotalCost());
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String name) {
        this.customerName = name;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String phone) {
        this.customerPhone = phone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String email) {
        this.customerEmail = email;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String address) {
        this.customerAddress = address;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime d) {
        this.orderDate = d;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal total) {
        this.totalAmount = total;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime c) {
        this.createdAt = c;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime u) {
        this.updatedAt = u;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    public void setLines(List<OrderLine> lines) {
        this.lines = lines;
    }
}
