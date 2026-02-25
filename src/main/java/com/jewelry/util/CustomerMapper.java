package com.jewelry.util;

import com.jewelry.dto.CustomerDTO;
import com.jewelry.entity.Customer;

/**
 * Stateless mapper between {@link Customer} entity and {@link CustomerDTO}.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> replace with a MapStruct
 * {@code @Mapper} interface — method signatures are intentionally compatible.
 */
public final class CustomerMapper {

    private CustomerMapper() {
        /* utility */ }

    /** Entity → DTO (for displaying in the UI). */
    public static CustomerDTO toDTO(Customer entity) {
        if (entity == null)
            return null;
        CustomerDTO dto = new CustomerDTO();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setAddress(entity.getAddress());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    /** DTO → Entity (for persistence). */
    public static Customer toEntity(CustomerDTO dto) {
        if (dto == null)
            return null;
        Customer entity = new Customer();
        entity.setId(dto.getId());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setAddress(dto.getAddress());
        entity.setNotes(dto.getNotes());
        return entity;
    }

    /** Updates an existing entity in-place (for update operations). */
    public static void updateEntity(CustomerDTO dto, Customer target) {
        target.setFirstName(dto.getFirstName());
        target.setLastName(dto.getLastName());
        target.setEmail(dto.getEmail());
        target.setPhone(dto.getPhone());
        target.setAddress(dto.getAddress());
        target.setNotes(dto.getNotes());
    }
}
