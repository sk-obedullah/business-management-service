package com.jewelry.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

/**
 * Utility class for Bean Validation.
 */
public class ValidationUtil {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    /**
     * Validates an object using Bean Validation annotations.
     * Throws an IllegalArgumentException with the first violation message if any are found.
     *
     * @param object the object to validate
     * @param <T>    the type of the object
     * @throws IllegalArgumentException if validation fails
     */
    public static <T> void validate(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Object to validate must not be null");
        }
        
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            ConstraintViolation<T> violation = violations.iterator().next();
            throw new IllegalArgumentException(violation.getMessage());
        }
    }
}
