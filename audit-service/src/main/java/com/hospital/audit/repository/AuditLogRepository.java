package com.hospital.audit.repository;

import com.hospital.audit.entity.AuditLog;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive repository for AuditLog entity.
 *
 * Audit Log Rules:
 * - Immutable: No updates or deletes allowed
 * - Append-only: Only persist() operations
 */
@ApplicationScoped
public class AuditLogRepository implements PanacheRepositoryBase<AuditLog, UUID> {

    /**
     * Find audit logs by tenant with pagination.
     */
    public Uni<List<AuditLog>> findByTenantPaginated(String tenantId, int page, int size) {
        return find("tenantId = ?1 ORDER BY timestamp DESC", tenantId)
            .page(Page.of(page, size))
            .list();
    }

    /**
     * Find audit logs by user.
     */
    public Uni<List<AuditLog>> findByUser(String tenantId, UUID userId, int page, int size) {
        return find("tenantId = ?1 AND userId = ?2 ORDER BY timestamp DESC", tenantId, userId)
            .page(Page.of(page, size))
            .list();
    }

    /**
     * Find audit logs by resource (return as list for now).
     */
    public Uni<List<AuditLog>> findByResource(String tenantId, String resourceType, UUID resourceId) {
        return find("tenantId = ?1 AND resourceType = ?2 AND resourceId = ?3 ORDER BY timestamp DESC",
            tenantId, resourceType, resourceId).list();
    }

    /**
     * Find audit logs by action type.
     */
    public Uni<List<AuditLog>> findByAction(String tenantId, String action, int page, int size) {
        return find("tenantId = ?1 AND action = ?2 ORDER BY timestamp DESC", tenantId, action)
            .page(Page.of(page, size))
            .list();
    }

    /**
     * Find audit logs within date range.
     */
    public Uni<List<AuditLog>> findByDateRange(String tenantId, LocalDateTime startDate,
                                                 LocalDateTime endDate, int page, int size) {
        return find("tenantId = ?1 AND timestamp >= ?2 AND timestamp <= ?3 ORDER BY timestamp DESC",
            tenantId, startDate, endDate)
            .page(Page.of(page, size))
            .list();
    }

    /**
     * Find recent audit logs (limited).
     */
    public Uni<List<AuditLog>> findRecent(String tenantId, int limit) {
        return find("tenantId = ?1 ORDER BY timestamp DESC", tenantId)
            .page(Page.ofSize(limit))
            .list();
    }

    /**
     * Find failed operations (HTTP status >= 400).
     */
    public Uni<List<AuditLog>> findFailedOperations(String tenantId, int page, int size) {
        return find("tenantId = ?1 AND statusCode >= 400 ORDER BY timestamp DESC", tenantId)
            .page(Page.of(page, size))
            .list();
    }

    /**
     * Search audit logs by description (case-insensitive).
     */
    public Uni<List<AuditLog>> searchByDescription(String tenantId, String searchTerm, int page, int size) {
        return find("tenantId = ?1 AND LOWER(description) LIKE LOWER(?2) ORDER BY timestamp DESC",
            tenantId, "%" + searchTerm + "%")
            .page(Page.of(page, size))
            .list();
    }

    /**
     * Count audit logs by tenant.
     */
    public Uni<Long> countByTenant(String tenantId) {
        return count("tenantId = ?1", tenantId);
    }

    /**
     * Count audit logs by action type.
     */
    public Uni<Long> countByAction(String tenantId, String action) {
        return count("tenantId = ?1 AND action = ?2", tenantId, action);
    }
}
