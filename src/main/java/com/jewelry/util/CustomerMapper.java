package com.jewelry.util;

import com.jewelry.dto.CustomerDTO;
import com.jewelry.entity.Customer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper between {@link Customer} entity and {@link CustomerDTO}.
 */
@Mapper
public interface CustomerMapper {

    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    /** Entity → DTO (for displaying in the UI). */
    CustomerDTO toDTO(Customer entity);

    /** DTO → Entity (for persistence). */
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerDTO dto);

    /** Updates an existing entity in-place (for update operations). */
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(CustomerDTO dto, @MappingTarget Customer target);
}
