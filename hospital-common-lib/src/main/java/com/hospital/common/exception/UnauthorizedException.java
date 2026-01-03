package com.hospital.common.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exception thrown when authentication fails.
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, Response.Status.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause, Response.Status.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
