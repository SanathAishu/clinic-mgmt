package com.hospital.common.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exception thrown when a resource already exists (conflict).
 */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(message, Response.Status.CONFLICT, "CONFLICT");
    }

    public ConflictException(String resourceType, String fieldName, String value) {
        super(
            String.format("%s with %s '%s' already exists", resourceType, fieldName, value),
            Response.Status.CONFLICT,
            "CONFLICT"
        );
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause, Response.Status.CONFLICT, "CONFLICT");
    }
}
