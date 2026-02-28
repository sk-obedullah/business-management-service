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
public record OrderDTO(
        Long id,
        Long customerId,
        String customerName,
        String customerPhone,
        String customerEmail,
        String customerAddress,
        LocalDateTime orderDate,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discount,
        String notes,
        List<OrderLineDTO> lines
) {

    // ── Derived ──────────────────────────────────────────────────────────────

    public BigDecimal getNetAmount() {
        BigDecimal d = (discount != null) ? discount : BigDecimal.ZERO;
        BigDecimal t = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
        return t.subtract(d);
    }

    public int getItemCount() {
        return lines != null ? lines.size() : 0;
    }
}
