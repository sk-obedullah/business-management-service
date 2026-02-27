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
        ProductDTO dto = new ProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSku(entity.getSku());
        dto.setCategory(entity.getCategory());
        dto.setMetal(entity.getMetal());
        dto.setPurity(entity.getPurity());
        dto.setWeightGrams(entity.getWeightGrams());
        dto.setCostPrice(entity.getCostPrice());
        dto.setSellingPrice(entity.getSellingPrice());
        dto.setQuantityOnHand(entity.getQuantityOnHand());
        dto.setDescription(entity.getDescription());
        dto.setImagePath(entity.getImagePath());
        return dto;
    }

    /** Converts a DTO back to an entity (for persistence). */
    public static Product toEntity(ProductDTO dto) {
        if (dto == null)
            return null;
        Product entity = new Product();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setSku(dto.getSku());
        entity.setCategory(dto.getCategory());
        entity.setMetal(dto.getMetal());
        entity.setPurity(dto.getPurity());
        entity.setWeightGrams(dto.getWeightGrams());
        entity.setCostPrice(dto.getCostPrice());
        entity.setSellingPrice(dto.getSellingPrice());
        entity.setQuantityOnHand(dto.getQuantityOnHand());
        entity.setDescription(dto.getDescription());
        entity.setImagePath(dto.getImagePath());
        return entity;
    }

    /** Updates an existing entity in-place from a DTO (for update operations). */
    public static void updateEntity(ProductDTO dto, Product target) {
        target.setName(dto.getName());
        target.setSku(dto.getSku());
        target.setCategory(dto.getCategory());
        target.setMetal(dto.getMetal());
        target.setPurity(dto.getPurity());
        target.setWeightGrams(dto.getWeightGrams());
        target.setCostPrice(dto.getCostPrice());
        target.setSellingPrice(dto.getSellingPrice());
        target.setQuantityOnHand(dto.getQuantityOnHand());
        target.setDescription(dto.getDescription());
        target.setImagePath(dto.getImagePath());
    }
}
