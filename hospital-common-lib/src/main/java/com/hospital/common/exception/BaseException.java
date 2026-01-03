package com.hospital.common.exception;

import jakarta.ws.rs.core.Response;

/**
 * Base exception for all application-specific exceptions.
 * Provides HTTP status code mapping for REST responses.
 */
public abstract class BaseException extends RuntimeException {

    private final Response.Status httpStatus;
    private final String errorCode;

    public BaseException(String message, Response.Status httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public BaseException(String message, Throwable cause, Response.Status httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public Response.Status getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return httpStatus.getStatusCode();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", errorCode, httpStatus, getMessage());
    }
}
