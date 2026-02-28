package com.jewelry.dto;

import java.math.BigDecimal;

/**
 * DTO for a single order line item used in the order form table.
 */
public record OrderLineDTO(
        Long productId,
        String productName,
        String productSku,
        int quantity,
        BigDecimal unitPrice, // selling_price snapshot
        BigDecimal costPrice, // cost_price snapshot
        int stockAvailable // transient — for UI validation only
) {

    // ── Derived ──────────────────────────────────────────────────────────────

    public BigDecimal getLineTotal() {
        if (unitPrice == null)
            return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
