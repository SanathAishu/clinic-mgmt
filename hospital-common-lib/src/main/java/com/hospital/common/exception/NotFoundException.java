package com.hospital.common.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exception thrown when a requested resource is not found.
 */
public class NotFoundException extends BaseException {

    public NotFoundException(String message) {
        super(message, Response.Status.NOT_FOUND, "NOT_FOUND");
    }

    public NotFoundException(String resourceType, String identifier) {
        super(
            String.format("%s with id '%s' not found", resourceType, identifier),
            Response.Status.NOT_FOUND,
            "NOT_FOUND"
        );
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause, Response.Status.NOT_FOUND, "NOT_FOUND");
    }
}
