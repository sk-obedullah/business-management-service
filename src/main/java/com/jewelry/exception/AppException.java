package com.jewelry.exception;

/**
 * Root of the application exception hierarchy.
 *
 * <p>All application-specific exceptions extend this class, allowing callers
 * to catch either a specific subtype or the entire application exception tree
 * via a single {@code catch (AppException e)} block.
 *
 * <p>Extends {@link RuntimeException} intentionally — checked exceptions add
 * noise at every layer boundary without providing meaningful recovery paths in
 * a desktop application. Services declare what can go wrong via Javadoc.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
