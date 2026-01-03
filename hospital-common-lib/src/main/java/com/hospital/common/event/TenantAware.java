package com.hospital.common.event;

/**
 * Marker interface for events that include tenant context.
 *
 * DESIGN PRINCIPLE: Interface-based contract instead of inheritance.
 * Events implement this interface rather than extending a base class,
 * promoting loose coupling and composition over inheritance.
 *
 * Usage:
 * ```java
 * public class PatientCreatedEvent implements TenantAware, Auditable {
 *     private final EventMetadata metadata;
 *     private final String tenantId;
 *     // ...
 * }
 * ```
 */
public interface TenantAware {
    /**
     * Get the tenant ID for this event.
     * Required for multi-tenant event isolation.
     *
     * @return Tenant identifier (never null)
     */
    String getTenantId();
}
