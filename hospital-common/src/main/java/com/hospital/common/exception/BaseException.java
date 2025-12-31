package com.hospital.common.exception;

import lombok.Getter;

/**
 * Base exception class for all custom exceptions
 */
@Getter
public class BaseException extends RuntimeException {
    private final String errorCode;

    public BaseException(String message) {
        super(message);
        this.errorCode = "INTERNAL_ERROR";
    }

    public BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INTERNAL_ERROR";
    }

    public BaseException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
