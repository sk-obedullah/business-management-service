package com.jewelry.util;

import com.jewelry.dto.ProductDTO;
import com.jewelry.entity.Product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper between {@link Product} entity and {@link ProductDTO}.
 */
@Mapper
public interface ProductMapper {

    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);
    /** Converts an entity to a DTO (for displaying in the UI). */
    ProductDTO toDTO(Product entity);

    /** Converts a DTO back to an entity (for persistence). */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductDTO dto);

    /** Updates an existing entity in-place from a DTO (for update operations). */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ProductDTO dto, @MappingTarget Product target);
}
