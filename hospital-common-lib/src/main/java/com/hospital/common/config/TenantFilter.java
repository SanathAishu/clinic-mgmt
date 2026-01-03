package com.hospital.common.config;

import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * JAX-RS filter to extract tenant ID from JWT token and set it in TenantContext.
 *
 * This filter:
 * 1. Runs after JWT authentication (Priority.USER)
 * 2. Extracts tenant_id claim from the validated JWT token
 * 3. Sets the tenant in TenantContext for use throughout the request
 * 4. Clears the context after response (to prevent thread pool contamination)
 *
 * NOTE: This filter only runs if a JWT token is present and valid.
 * Public endpoints without authentication will not have tenant context set.
 */
@Provider
@Priority(jakarta.ws.rs.Priorities.USER)
public class TenantFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            // Only set tenant context if JWT is present (authenticated request)
            if (jwt != null && jwt.getSubject() != null) {
                String tenantId = jwt.getClaim("tenant_id");

                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.setCurrentTenant(tenantId);
                    Log.debugf("Tenant context set from JWT: %s (user: %s)", tenantId, jwt.getSubject());
                } else {
                    Log.warnf("JWT token present but tenant_id claim is missing for user: %s", jwt.getSubject());
                }
            }
        } catch (Exception e) {
            // JWT not available (public endpoint) or error extracting tenant
            Log.debugf("No tenant context set for request: %s %s",
                    requestContext.getMethod(), requestContext.getUriInfo().getPath());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // CRITICAL: Clear tenant context after request completion
        // This prevents tenant data leakage across requests in thread pools
        TenantContext.clear();
    }
}
