package com.hospital.common.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class NotFoundException extends BaseException {
    public NotFoundException(String message) {
        super(message, "NOT_FOUND");
    }

    public NotFoundException(String resourceType, Object id) {
        super(String.format("%s not found with id: %s", resourceType, id), "NOT_FOUND");
    }
}
