package com.jewelry.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for the Customer entity — used exclusively in the
 * presentation layer. Keeps the entity free of JavaFX or UI concerns.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> add {@code @Valid} and Bean
 * Validation constraints ({@code @Email}, {@code @NotBlank}, etc.).
 */
public record CustomerDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String notes,
        java.time.LocalDateTime createdAt
) {
    /** Convenience full-name for display in TableView cells. */
    public String getFullName() {
        return (firstName != null ? firstName : "")
                + (lastName != null ? " " + lastName : "");
    }
}
