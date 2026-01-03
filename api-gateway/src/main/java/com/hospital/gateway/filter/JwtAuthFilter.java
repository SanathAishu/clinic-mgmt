package com.hospital.gateway.filter;

import io.quarkus.logging.Log;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * JWT Authentication Filter for API Gateway.
 *
 * Responsibilities:
 * 1. Extract JWT token from Authorization header
 * 2. Validate JWT signature and claims
 * 3. Extract tenantId from JWT and add to request headers
 * 4. Extract user context (userId, email, roles, permissions)
 * 5. Add context to headers for backend services
 *
 * Flow:
 * Client -> Gateway (JWT in Authorization header)
 *   -> Gateway validates JWT
 *   -> Gateway extracts tenantId, userId, roles, permissions
 *   -> Gateway forwards request with headers:
 *      - Authorization: Bearer <token>
 *      - X-Tenant-Id: <tenantId>
 *      - X-User-Id: <userId>
 *      - X-User-Email: <email>
 *      - X-User-Roles: <comma-separated roles>
 *   -> Backend services use headers for context
 */
@ApplicationScoped
public class JwtAuthFilter {

    @Inject
    JWTParser jwtParser;

    public boolean authenticate(RoutingContext context) {
        HttpServerRequest request = context.request();

        // Extract Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Log.warnf("Missing or invalid Authorization header for path: %s", request.path());
            context.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"Missing or invalid Authorization header\"}");
            return false;
        }

        // Extract token
        String token = authHeader.substring(7); // Remove "Bearer " prefix

        try {
            // Parse and validate JWT
            JsonWebToken jwt = jwtParser.parse(token);

            // Extract claims
            String tenantId = jwt.getClaim("tenantId");
            String userId = jwt.getSubject();
            String email = jwt.getClaim("email");
            String roles = jwt.getClaim("roles"); // Comma-separated roles
            String permissions = jwt.getClaim("permissions"); // Comma-separated permissions

            // Validate required claims
            if (tenantId == null || tenantId.isBlank()) {
                Log.errorf("JWT missing required claim 'tenantId' for user: %s", userId);
                context.response()
                        .setStatusCode(401)
                        .putHeader("Content-Type", "application/json")
                        .end("{\"error\":\"Invalid token: missing tenantId\"}");
                return false;
            }

            // Add context headers for backend services
            request.headers().add("X-Tenant-Id", tenantId);
            request.headers().add("X-User-Id", userId);
            if (email != null) {
                request.headers().add("X-User-Email", email);
            }
            if (roles != null) {
                request.headers().add("X-User-Roles", roles);
            }
            if (permissions != null) {
                request.headers().add("X-User-Permissions", permissions);
            }

            // Add request ID for tracing
            String requestId = java.util.UUID.randomUUID().toString();
            request.headers().add("X-Request-Id", requestId);

            Log.debugf("Authenticated request: tenantId=%s, userId=%s, path=%s, requestId=%s",
                    tenantId, userId, request.path(), requestId);

            return true;

        } catch (ParseException e) {
            Log.errorf(e, "JWT parsing failed for path: %s", request.path());
            context.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"Invalid token\"}");
            return false;
        } catch (Exception e) {
            Log.errorf(e, "Unexpected error during JWT validation for path: %s", request.path());
            context.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"Internal server error\"}");
            return false;
        }
    }

    public boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/q/health") ||
               path.startsWith("/q/metrics") ||
               path.startsWith("/q/openapi") ||
               path.startsWith("/swagger-ui") ||
               path.equals("/");
    }
}
