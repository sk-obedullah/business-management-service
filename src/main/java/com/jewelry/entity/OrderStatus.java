package com.jewelry.entity;

/**
 * Lifecycle states for an {@link Order}.
 *
 * <p>
 * Valid transitions:
 * 
 * <pre>
 *   PENDING → PROCESSING → COMPLETED
 *   PENDING → CANCELLED
 *   PROCESSING → CANCELLED
 * </pre>
 *
 * <p>
 * Stored as a VARCHAR ENUM in MySQL ({@code order.status}).
 * The string value matches the DB enum literal exactly.
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> annotate with
 * {@code @Enumerated(EnumType.STRING)} on the entity field.
 */
public enum OrderStatus {

    PENDING("Pending"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Returns the enum constant matching the given DB string, case-insensitive. */
    public static OrderStatus fromString(String value) {
        for (OrderStatus s : values()) {
            if (s.name().equalsIgnoreCase(value))
                return s;
        }
        throw new IllegalArgumentException("Unknown OrderStatus: " + value);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
