package com.hospital.common.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exception thrown when input validation fails.
 */
public class ValidationException extends BaseException {

    private final String fieldName;

    public ValidationException(String message) {
        super(message, Response.Status.BAD_REQUEST, "VALIDATION_ERROR");
        this.fieldName = null;
    }

    public ValidationException(String fieldName, String message) {
        super(
            String.format("Validation error for field '%s': %s", fieldName, message),
            Response.Status.BAD_REQUEST,
            "VALIDATION_ERROR"
        );
        this.fieldName = fieldName;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause, Response.Status.BAD_REQUEST, "VALIDATION_ERROR");
        this.fieldName = null;
    }

    public String getFieldName() {
        return fieldName;
    }
}
