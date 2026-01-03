package com.hospital.common.exception;

import jakarta.ws.rs.core.Response;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, Response.Status.FORBIDDEN, "FORBIDDEN");
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause, Response.Status.FORBIDDEN, "FORBIDDEN");
    }
}
