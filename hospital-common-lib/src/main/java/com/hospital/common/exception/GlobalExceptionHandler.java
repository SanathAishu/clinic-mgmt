package com.hospital.common.exception;
import io.quarkus.logging.Log;

import com.hospital.common.dto.ErrorResponse;
import io.quarkus.arc.log.LoggerName;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

/**
 * Global exception handler for all microservices.
 * Maps exceptions to standardized ErrorResponse DTOs.
 *
 * Usage: Include hospital-common-quarkus dependency in your service.
 * This handler will automatically be discovered and applied.
 */
@ApplicationScoped
public class GlobalExceptionHandler {

    
    Logger logger;

    /**
     * Handle custom BaseException and its subclasses
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleBaseException(BaseException ex, @Context UriInfo uriInfo) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        Log.warnf("Business exception occurred: [%s] %s at path: %s",
                ex.getErrorCode(), ex.getMessage(), path);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ex.getStatusCode())
                .error(ex.getHttpStatus().getReasonPhrase())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(path)
                .build();

        return RestResponse.status(ex.getHttpStatus(), errorResponse);
    }

    /**
     * Handle NotFoundException (404)
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleNotFoundException(NotFoundException ex, @Context UriInfo uriInfo) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        Log.warnf("Resource not found: %s at path: %s", ex.getMessage(), path);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(404)
                .error("Not Found")
                .errorCode("NOT_FOUND")
                .message(ex.getMessage())
                .path(path)
                .build();

        return RestResponse.status(Response.Status.NOT_FOUND, errorResponse);
    }

    /**
     * Handle ValidationException (400)
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleValidationException(ValidationException ex, @Context UriInfo uriInfo) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        Log.warnf("Validation error: %s at path: %s", ex.getMessage(), path);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .errorCode("VALIDATION_ERROR")
                .message(ex.getMessage())
                .path(path)
                .build();

        // Add field error if present
        if (ex.getFieldName() != null) {
            errorResponse.setFieldErrors(java.util.List.of(
                    new ErrorResponse.FieldError(ex.getFieldName(), ex.getMessage(), null)
            ));
        }

        return RestResponse.status(Response.Status.BAD_REQUEST, errorResponse);
    }

    /**
     * Handle ConflictException (409)
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleConflictException(ConflictException ex, @Context UriInfo uriInfo) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        Log.warnf("Conflict error: %s at path: %s", ex.getMessage(), path);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(409)
                .error("Conflict")
                .errorCode("CONFLICT")
                .message(ex.getMessage())
                .path(path)
                .build();

        return RestResponse.status(Response.Status.CONFLICT, errorResponse);
    }

    /**
     * Handle UnauthorizedException (401)
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex, @Context UriInfo uriInfo) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        Log.warnf("Unauthorized access: %s at path: %s", ex.getMessage(), path);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(401)
                .error("Unauthorized")
                .errorCode("UNAUTHORIZED")
                .message(ex.getMessage())
                .path(path)
                .build();

        return RestResponse.status(Response.Status.UNAUTHORIZED, errorResponse);
    }

    /**
     * Handle JAX-RS WebApplicationException (generic REST errors)
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleWebApplicationException(WebApplicationException ex, @Context UriInfo uriInfo) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        Log.warnf("Web application exception: %s at path: %s", ex.getMessage(), path);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ex.getResponse().getStatus())
                .error(Response.Status.fromStatusCode(ex.getResponse().getStatus()).getReasonPhrase())
                .errorCode("WEB_ERROR")
                .message(ex.getMessage() != null ? ex.getMessage() : "Request processing failed")
                .path(path)
                .build();

        return RestResponse.status(Response.Status.fromStatusCode(ex.getResponse().getStatus()), errorResponse);
    }

    /**
     * Handle IllegalArgumentException (400 - Bad Request)
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, @Context UriInfo uriInfo) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        Log.warnf("Illegal argument: %s at path: %s", ex.getMessage(), path);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .errorCode("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .path(path)
                .build();

        return RestResponse.status(Response.Status.BAD_REQUEST, errorResponse);
    }

    /**
     * Handle all other exceptions (500 - Internal Server Error)
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleGenericException(Exception ex, @Context UriInfo uriInfo) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        // Log full stack trace for debugging
        Log.errorf(ex, "Unexpected error occurred at path: %s", path);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(500)
                .error("Internal Server Error")
                .errorCode("INTERNAL_ERROR")
                .message("An unexpected error occurred. Please contact support.")
                .path(path)
                .build();

        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, errorResponse);
    }
}
