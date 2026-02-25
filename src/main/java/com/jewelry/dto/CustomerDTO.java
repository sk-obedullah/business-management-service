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
public class CustomerDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String notes;
    private LocalDateTime createdAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public CustomerDTO() {
    }

    // ── Derived ──────────────────────────────────────────────────────────────

    /** Convenience full-name for display in TableView cells. */
    public String getFullName() {
        return (firstName != null ? firstName : "")
                + (lastName != null ? " " + lastName : "");
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime c) {
        this.createdAt = c;
    }
}
