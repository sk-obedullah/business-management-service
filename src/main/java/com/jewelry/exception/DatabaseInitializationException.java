package com.jewelry.exception;

/**
 * Thrown when the HikariCP pool cannot be created or a test connection fails.
 * Surfaced immediately at startup to give the user a clear failure message.
 */
public class DatabaseInitializationException extends AppException {

    public DatabaseInitializationException(String message) {
        super(message);
    }

    public DatabaseInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
