package com.jewelry.util;

import com.jewelry.dto.ProductDTO;
import com.jewelry.entity.Product;

/**
 * Stateless mapper between {@link Product} entity and {@link ProductDTO}.
 *
 * <p>
 * Centralising mapping here keeps both the entity and DTO free of
 * cross-layer dependencies.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> replace with MapStruct
 * {@code @Mapper} interface — method signatures are compatible.
 */
public final class ProductMapper {

    private ProductMapper() {
        /* utility */ }

    /** Converts an entity to a DTO (for displaying in the UI). */
    public static ProductDTO toDTO(Product entity) {
        if (entity == null)
            return null;
        return new ProductDTO(
                entity.getId(),
                entity.getName(),
                entity.getSku(),
                entity.getCategory(),
                entity.getMetal(),
                entity.getPurity(),
                entity.getWeightGrams(),
                entity.getCostPrice(),
                entity.getSellingPrice(),
                entity.getQuantityOnHand(),
                entity.getDescription(),
                entity.getImagePath()
        );
    }

    /** Converts a DTO back to an entity (for persistence). */
    public static Product toEntity(ProductDTO dto) {
        if (dto == null)
            return null;
        Product entity = new Product();
        entity.setId(dto.id());
        entity.setName(dto.name());
        entity.setSku(dto.sku());
        entity.setCategory(dto.category());
        entity.setMetal(dto.metal());
        entity.setPurity(dto.purity());
        entity.setWeightGrams(dto.weightGrams());
        entity.setCostPrice(dto.costPrice());
        entity.setSellingPrice(dto.sellingPrice());
        entity.setQuantityOnHand(dto.quantityOnHand());
        entity.setDescription(dto.description());
        entity.setImagePath(dto.imagePath());
        return entity;
    }

    /** Updates an existing entity in-place from a DTO (for update operations). */
    public static void updateEntity(ProductDTO dto, Product target) {
        target.setName(dto.name());
        target.setSku(dto.sku());
        target.setCategory(dto.category());
        target.setMetal(dto.metal());
        target.setPurity(dto.purity());
        target.setWeightGrams(dto.weightGrams());
        target.setCostPrice(dto.costPrice());
        target.setSellingPrice(dto.sellingPrice());
        target.setQuantityOnHand(dto.quantityOnHand());
        target.setDescription(dto.description());
        target.setImagePath(dto.imagePath());
    }
}
