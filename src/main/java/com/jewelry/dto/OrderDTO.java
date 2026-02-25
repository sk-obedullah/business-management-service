package com.jewelry.dto;

import com.jewelry.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing an Order in the presentation layer.
 * Carries denormalised customer name and computed total/profit fields
 * so the TableView and form never need to call back into the service.
 */
public class OrderDTO {

    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private String notes;
    private List<OrderLineDTO> lines = new ArrayList<>();

    // ── Derived ──────────────────────────────────────────────────────────────

    public BigDecimal getNetAmount() {
        BigDecimal d = (discount != null) ? discount : BigDecimal.ZERO;
        BigDecimal t = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
        return t.subtract(d);
    }

    public int getItemCount() {
        return lines.size();
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

    public void setCustomerId(Long id) {
        this.customerId = id;
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

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
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

    public void setTotalAmount(BigDecimal t) {
        this.totalAmount = t;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal d) {
        this.discount = d;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<OrderLineDTO> getLines() {
        return lines;
    }

    public void setLines(List<OrderLineDTO> lines) {
        this.lines = lines;
    }
}
