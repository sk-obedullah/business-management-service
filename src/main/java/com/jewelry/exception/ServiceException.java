package com.jewelry.exception;

/**
 * Wraps unexpected failures inside the service layer, e.g., a repository
 * throws {@link java.sql.SQLException} that is propagated upward.
 */
public class ServiceException extends AppException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
