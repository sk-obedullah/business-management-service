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
        return new CustomerDTO(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }

    /** DTO → Entity (for persistence). */
    public static Customer toEntity(CustomerDTO dto) {
        if (dto == null)
            return null;
        Customer entity = new Customer();
        entity.setId(dto.id());
        entity.setFirstName(dto.firstName());
        entity.setLastName(dto.lastName());
        entity.setEmail(dto.email());
        entity.setPhone(dto.phone());
        entity.setAddress(dto.address());
        entity.setNotes(dto.notes());
        return entity;
    }

    /** Updates an existing entity in-place (for update operations). */
    public static void updateEntity(CustomerDTO dto, Customer target) {
        target.setFirstName(dto.firstName());
        target.setLastName(dto.lastName());
        target.setEmail(dto.email());
        target.setPhone(dto.phone());
        target.setAddress(dto.address());
        target.setNotes(dto.notes());
    }
}
