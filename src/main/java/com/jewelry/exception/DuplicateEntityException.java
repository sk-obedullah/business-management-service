package com.jewelry.exception;

/**
 * Thrown when an insert would violate a uniqueness constraint, e.g.,
 * duplicate SKU or duplicate customer email.
 */
public class DuplicateEntityException extends AppException {

    public DuplicateEntityException(String message) {
        super(message);
    }

    public DuplicateEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
