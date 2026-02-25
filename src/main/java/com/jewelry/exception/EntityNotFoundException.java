package com.jewelry.exception;

/**
 * Thrown by a service or repository when a requested entity does not exist.
 *
 * <p>Example: {@code productService.findById(99)} where ID 99 is absent.
 */
public class EntityNotFoundException extends AppException {

    public EntityNotFoundException(String entityName, Object id) {
        super(entityName + " not found with id: " + id);
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
