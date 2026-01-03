package com.hospital.common.config;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Thread-local storage for tenant context.
 * Stores the current tenant ID for the duration of a request to enable tenant isolation.
 *
 * Usage:
 * 1. Extract tenantId from JWT token in request filter/interceptor
 * 2. Call TenantContext.setCurrentTenant(tenantId)
 * 3. Use TenantContext.getCurrentTenant() throughout the request
 * 4. Clear context after request completion
 *
 * This context is used by:
 * - Hibernate Filters to automatically filter queries by tenant
 * - Cache key generation to isolate cache entries by tenant
 * - Event publishing to include tenant information
 * - Audit logging to track tenant-specific operations
 */
@ApplicationScoped
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Set the current tenant ID for this request/thread.
     *
     * @param tenantId The tenant identifier (e.g., "hosp-001")
     */
    public static void setCurrentTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            Log.warn("Attempting to set null or blank tenant ID");
            return;
        }
        CURRENT_TENANT.set(tenantId);
        Log.debug("Tenant context set to: " + tenantId);
    }

    /**
     * Get the current tenant ID for this request/thread.
     *
     * @return The tenant identifier, or null if not set
     */
    public static String getCurrentTenant() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            Log.warn("Tenant context not set - this may indicate a missing tenant filter");
        }
        return tenantId;
    }

    /**
     * Get the current tenant ID, throwing an exception if not set.
     * Use this when tenant context is required and you want to fail fast.
     *
     * @return The tenant identifier
     * @throws IllegalStateException if tenant context is not set
     */
    public static String getCurrentTenantOrThrow() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set - tenant ID is required for this operation");
        }
        return tenantId;
    }

    /**
     * Clear the current tenant context.
     * IMPORTANT: This must be called after request completion to prevent thread pool contamination.
     * Best practice: Call this in a finally block or use a request filter.
     */
    public static void clear() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId != null) {
            Log.debug("Clearing tenant context for: " + tenantId);
        }
        CURRENT_TENANT.remove();
    }

    /**
     * Check if tenant context is currently set.
     *
     * @return true if tenant context is set, false otherwise
     */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Execute a task with a specific tenant context.
     * The context will be automatically cleared after execution.
     *
     * @param tenantId The tenant identifier
     * @param task     The task to execute
     */
    public static void executeWithTenant(String tenantId, Runnable task) {
        try {
            setCurrentTenant(tenantId);
            task.run();
        } finally {
            clear();
        }
    }

    /**
     * Execute a task with a specific tenant context and return a result.
     * The context will be automatically cleared after execution.
     *
     * @param tenantId The tenant identifier
     * @param supplier The function to execute
     * @param <T>      The return type
     * @return The result of the function
     */
    public static <T> T executeWithTenant(String tenantId, java.util.function.Supplier<T> supplier) {
        try {
            setCurrentTenant(tenantId);
            return supplier.get();
        } finally {
            clear();
        }
    }
}
