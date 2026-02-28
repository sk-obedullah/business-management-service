package com.jewelry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a jewelry product in the catalogue.
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required.")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "SKU is required.")
    @Column(nullable = false, unique = true)
    private String sku;

    @NotBlank(message = "Category is required.")
    @Column(nullable = false)
    private String category;

    @NotBlank(message = "Metal is required.")
    @Column(nullable = false)
    private String metal;

    private String purity;

    @Column(name = "weight_grams", precision = 10, scale = 3)
    private BigDecimal weightGrams;

    @NotNull(message = "Cost price is required.")
    @PositiveOrZero(message = "Cost price cannot be negative.")
    @Column(name = "cost_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required.")
    @Positive(message = "Selling price must be positive.")
    @Column(name = "selling_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Lifecycle callbacks ───────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
