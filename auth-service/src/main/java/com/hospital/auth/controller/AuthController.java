package com.hospital.auth.controller;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.LoginResponse;
import com.hospital.auth.dto.RegisterRequest;
import com.hospital.auth.dto.UserDto;
import com.hospital.auth.service.AuthService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Authentication REST controller.
 *
 * Public Endpoints (no JWT required):
 * - POST /api/auth/login - User login
 * - POST /api/auth/register - User registration
 *
 * Multi-Tenancy:
 * - tenantId extracted from X-Tenant-Id header (set by API Gateway)
 * - For direct testing, uses default tenant if header missing
 *
 * Security:
 * - Login rate-limited by API Gateway
 * - Registration may require invitation code (future)
 * - Passwords validated via Bean Validation
 */
@Path("/api/auth")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    AuthService authService;

    @ConfigProperty(name = "tenant.default-tenant-id", defaultValue = "default-tenant")
    String defaultTenantId;

    /**
     * User login endpoint.
     *
     * Request:
     * {
     *   "email": "user@hospital.com",
     *   "password": "SecurePass123!"
     * }
     *
     * Response:
     * {
     *   "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 86400,
     *   "user": {
     *     "id": "uuid",
     *     "name": "John Doe",
     *     "email": "user@hospital.com",
     *     "roles": ["DOCTOR"],
     *     "permissions": ["patient:read", "appointment:create"]
     *   }
     * }
     *
     * @param tenantId Tenant ID from header (or default)
     * @param request Login credentials
     * @return Login response with JWT token
     */
    @POST
    @Path("/login")
    public Uni<Response> login(
        @HeaderParam("X-Tenant-Id") String tenantId,
        @Valid LoginRequest request
    ) {
        // Use default tenant if header not provided (for testing)
        String resolvedTenantId = (tenantId != null && !tenantId.isBlank())
            ? tenantId
            : defaultTenantId;

        Log.infof("Login attempt for email: %s, tenant: %s", request.getEmail(), resolvedTenantId);

        return authService.login(resolvedTenantId, request)
            .map(loginResponse -> Response.ok(loginResponse).build())
            .onFailure().recoverWithItem(error -> {
                Log.errorf(error, "Login failed for email: %s", request.getEmail());
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse(error.getMessage()))
                    .build();
            });
    }

    /**
     * User registration endpoint.
     *
     * Request:
     * {
     *   "name": "John Doe",
     *   "email": "john@hospital.com",
     *   "password": "SecurePass123!",
     *   "phone": "+1234567890"
     * }
     *
     * Response:
     * {
     *   "id": "uuid",
     *   "name": "John Doe",
     *   "email": "john@hospital.com",
     *   "active": true,
     *   "emailVerified": false
     * }
     *
     * @param tenantId Tenant ID from header (or default)
     * @param request Registration data
     * @return Created user DTO
     */
    @POST
    @Path("/register")
    public Uni<Response> register(
        @HeaderParam("X-Tenant-Id") String tenantId,
        @Valid RegisterRequest request
    ) {
        // Use default tenant if header not provided (for testing)
        String resolvedTenantId = (tenantId != null && !tenantId.isBlank())
            ? tenantId
            : defaultTenantId;

        Log.infof("Registration attempt for email: %s, tenant: %s", request.getEmail(), resolvedTenantId);

        return authService.register(resolvedTenantId, request)
            .map(userDto -> Response.status(Response.Status.CREATED).entity(userDto).build())
            .onFailure().recoverWithItem(error -> {
                Log.errorf(error, "Registration failed for email: %s", request.getEmail());

                // Determine appropriate HTTP status
                int status = error instanceof com.hospital.common.exception.ForbiddenException
                    ? Response.Status.CONFLICT.getStatusCode()
                    : Response.Status.BAD_REQUEST.getStatusCode();

                return Response.status(status)
                    .entity(new ErrorResponse(error.getMessage()))
                    .build();
            });
    }

    /**
     * Health check endpoint for auth service.
     *
     * @return OK status
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(new HealthResponse("UP", "auth-service")).build();
    }

    /**
     * Error response DTO.
     */
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    /**
     * Health response DTO.
     */
    public static class HealthResponse {
        public String status;
        public String service;

        public HealthResponse(String status, String service) {
            this.status = status;
            this.service = service;
        }
    }
}
