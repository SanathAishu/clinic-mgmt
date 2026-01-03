package com.hospital.common.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exception thrown when user is authenticated but lacks permissions (403 Forbidden).
 *
 * Usage:
 * - Valid JWT but insufficient permissions
 * - Account locked or disabled
 * - Resource access denied
 * - Tenant isolation violation
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, Response.Status.FORBIDDEN, "FORBIDDEN");
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause, Response.Status.FORBIDDEN, "FORBIDDEN");
    }
}
