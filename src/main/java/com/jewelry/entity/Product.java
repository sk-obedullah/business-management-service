package com.jewelry.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a jewelry product in the catalogue.
 *
 * <p>
 * This is a plain entity class — no ORM annotations, no framework dependency.
 * Fields map 1-to-1 to the {@code product} table columns.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> annotate with {@code @Entity},
 * {@code @Table}, and add {@code @Id @GeneratedValue} to {@code id}. The field
 * names already follow JPA naming conventions.
 */
public class Product {

    private Long id;
    private String name;
    private String sku; // unique catalogue code e.g. "RING-AU-018K-001"
    private String category; // Ring, Necklace, Bracelet, Earring, etc.
    private String metal; // Gold, Silver, Platinum, etc.
    private String purity; // 18K, 22K, 925, etc.
    private BigDecimal weightGrams;
    private BigDecimal costPrice; // what we paid / cost of making
    private BigDecimal sellingPrice; // listed sale price
    private int quantityOnHand;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Product() {
    }

    public Product(Long id, String name, String sku, String category, String metal,
            String purity, BigDecimal weightGrams, BigDecimal costPrice,
            BigDecimal sellingPrice, int quantityOnHand, String description,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.metal = metal;
        this.purity = purity;
        this.weightGrams = weightGrams;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.quantityOnHand = quantityOnHand;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public void setWeightGrams(BigDecimal weightGrams) {
        this.weightGrams = weightGrams;
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

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", sku='" + sku + "', name='" + name + "'}";
    }
}
