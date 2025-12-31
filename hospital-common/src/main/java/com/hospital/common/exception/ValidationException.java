package com.hospital.common.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation fails
 */
@Getter
public class ValidationException extends BaseException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String field, String fieldMessage) {
        super("Validation failed", "VALIDATION_ERROR");
        this.fieldErrors = new HashMap<>();
        this.fieldErrors.put(field, fieldMessage);
    }
}
