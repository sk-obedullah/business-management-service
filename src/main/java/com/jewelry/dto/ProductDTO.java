package com.jewelry.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object for the Product entity used in the presentation layer.
 *
 * <p>
 * Using a DTO instead of the raw entity in controllers/FXML provides:
 * <ul>
 * <li>Clean separation between persistence model and UI model</li>
 * <li>JavaFX-friendly String fields for editable form fields</li>
 * <li>Computed fields (e.g., profitMargin) without polluting the entity</li>
 * </ul>
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> annotate with {@code @Valid}
 * and add Bean Validation annotations ({@code @NotBlank}, {@code @DecimalMin}).
 */
public record ProductDTO(
        Long id,
        String name,
        String sku,
        String category,
        String metal,
        String purity,
        BigDecimal weightGrams,
        BigDecimal costPrice,
        BigDecimal sellingPrice,
        int quantityOnHand,
        String description,
        String imagePath
) {
    /**
     * Gross profit per unit in absolute currency.
     * Returns {@link BigDecimal#ZERO} if either price is null.
     */
    public BigDecimal getUnitProfit() {
        if (costPrice == null || sellingPrice == null)
            return BigDecimal.ZERO;
        return sellingPrice.subtract(costPrice);
    }

    /**
     * Gross margin as a percentage: {@code (sell - cost) / sell * 100}.
     * Returns {@link BigDecimal#ZERO} if selling price is null or zero.
     */
    public BigDecimal getProfitMarginPercent() {
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (costPrice == null)
            return BigDecimal.ZERO;
        return sellingPrice.subtract(costPrice)
                .divide(sellingPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
