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
public class ProductDTO {

    private Long id;
    private String name;
    private String sku;
    private String category;
    private String metal;
    private String purity;
    private BigDecimal weightGrams;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private int quantityOnHand;
    private String description;
    private String imagePath;

    // ── Constructors ─────────────────────────────────────────────────────────

    public ProductDTO() {
    }

    // ── Computed / derived ───────────────────────────────────────────────────

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

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMetal() {
        return metal;
    }

    public void setMetal(String metal) {
        this.metal = metal;
    }

    public String getPurity() {
        return purity;
    }

    public void setPurity(String purity) {
        this.purity = purity;
    }

    public BigDecimal getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(BigDecimal w) {
        this.weightGrams = w;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal p) {
        this.sellingPrice = p;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int qty) {
        this.quantityOnHand = qty;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
