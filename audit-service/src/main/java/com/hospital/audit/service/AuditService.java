package com.hospital.audit.service;

import com.hospital.audit.dto.AuditLogDto;
import com.hospital.audit.entity.AuditLog;
import com.hospital.audit.repository.AuditLogRepository;
import com.hospital.common.rbac.service.PermissionService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for querying and managing audit logs.
 *
 * Security:
 * - All operations require admin permissions
 * - Audit logs are immutable (no updates/deletes)
 * - Tenant isolation enforced
 *
 * RBAC:
 * - Requires "audit:read" permission for queries
 * - Only admins can access audit logs
 */
@ApplicationScoped
public class AuditService {

    @Inject
    AuditLogRepository auditLogRepository;

    @Inject
    PermissionService permissionService;

    public Uni<List<AuditLogDto>> getAuditLogs(String tenantId, int page, int size) {
        return permissionService.requirePermission("audit:read")
            .chain(() -> auditLogRepository.findByTenantPaginated(tenantId, page, size))
            .map(logs -> logs.stream().map(this::toDto).toList());
    }

    public Uni<List<AuditLogDto>> getAuditLogsByUser(String tenantId, UUID userId, int page, int size) {
        return permissionService.requirePermission("audit:read")
            .chain(() -> auditLogRepository.findByUser(tenantId, userId, page, size))
            .map(logs -> logs.stream().map(this::toDto).toList());
    }

    public Uni<List<AuditLogDto>> getResourceHistory(String tenantId, String resourceType, UUID resourceId) {
        return permissionService.requirePermission("audit:read")
            .chain(() -> auditLogRepository.findByResource(tenantId, resourceType, resourceId))
            .map(logs -> logs.stream().map(this::toDto).toList());
    }

    public Uni<List<AuditLogDto>> getAuditLogsByAction(String tenantId, String action, int page, int size) {
        return permissionService.requirePermission("audit:read")
            .chain(() -> auditLogRepository.findByAction(tenantId, action, page, size))
            .map(logs -> logs.stream().map(this::toDto).toList());
    }

    public Uni<List<AuditLogDto>> getAuditLogsByDateRange(String tenantId, LocalDateTime startDate,
                                                           LocalDateTime endDate, int page, int size) {
        return permissionService.requirePermission("audit:read")
            .chain(() -> auditLogRepository.findByDateRange(tenantId, startDate, endDate, page, size))
            .map(logs -> logs.stream().map(this::toDto).toList());
    }

    public Uni<List<AuditLogDto>> getRecentAuditLogs(String tenantId, int limit) {
        return permissionService.requirePermission("audit:read")
            .chain(() -> auditLogRepository.findRecent(tenantId, limit))
            .map(logs -> logs.stream().map(this::toDto).toList());
    }

    public Uni<List<AuditLogDto>> getFailedOperations(String tenantId, int page, int size) {
        return permissionService.requirePermission("audit:read")
            .chain(() -> auditLogRepository.findFailedOperations(tenantId, page, size))
            .map(logs -> logs.stream().map(this::toDto).toList());
    }

    public Uni<List<AuditLogDto>> searchAuditLogs(String tenantId, String searchTerm, int page, int size) {
        return permissionService.requirePermission("audit:read")
            .chain(() -> auditLogRepository.searchByDescription(tenantId, searchTerm, page, size))
            .map(logs -> logs.stream().map(this::toDto).toList());
    }

    public Uni<AuditStatistics> getAuditStatistics(String tenantId) {
        return permissionService.requirePermission("audit:read")
            .chain(() -> Uni.combine().all()
                .unis(
                    auditLogRepository.countByTenant(tenantId),
                    auditLogRepository.countByAction(tenantId, "CREATE"),
                    auditLogRepository.countByAction(tenantId, "UPDATE"),
                    auditLogRepository.countByAction(tenantId, "DELETE"),
                    auditLogRepository.findFailedOperations(tenantId, 0, 1)
                        .map(List::size)
                )
                .asTuple()
                .map(tuple -> new AuditStatistics(
                    tuple.getItem1(), // total
                    tuple.getItem2(), // creates
                    tuple.getItem3(), // updates
                    tuple.getItem4(), // deletes
                    tuple.getItem5().longValue() // failed
                ))
            );
    }

    private AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(
            log.getId(),
            log.getTenantId(),
            log.getUserId(),
            log.getUserEmail(),
            log.getAction(),
            log.getResourceType(),
            log.getResourceId(),
            log.getDescription(),
            log.getOldValue(),
            log.getNewValue(),
            log.getIpAddress(),
            log.getUserAgent(),
            log.getEventId(),
            log.getHttpMethod(),
            log.getRequestPath(),
            log.getStatusCode(),
            log.getTimestamp()
        );
    }

    public static class AuditStatistics {
        public final long total;
        public final long creates;
        public final long updates;
        public final long deletes;
        public final long failed;

        public AuditStatistics(long total, long creates, long updates, long deletes, long failed) {
            this.total = total;
            this.creates = creates;
            this.updates = updates;
            this.deletes = deletes;
            this.failed = failed;
        }
    }
}
