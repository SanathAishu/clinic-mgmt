package com.hospital.common.compliance.repository;

import com.hospital.common.compliance.entity.DataBreachLog;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for DataBreachLog entity.
 */
@ApplicationScoped
public class DataBreachLogRepository implements PanacheRepositoryBase<DataBreachLog, UUID> {

    public Uni<DataBreachLog> findByIncidentId(String incidentId) {
        return find("incidentId", incidentId).firstResult();
    }

    public Uni<List<DataBreachLog>> findByTenant(String tenantId) {
        return list("tenantId = ?1 order by detectedAt desc", tenantId);
    }

    public Uni<List<DataBreachLog>> findBySeverity(String tenantId, DataBreachLog.BreachSeverity severity) {
        return list("tenantId = ?1 and severity = ?2 order by detectedAt desc",
                tenantId, severity);
    }

    public Uni<List<DataBreachLog>> findByStatus(String tenantId, DataBreachLog.BreachStatus status) {
        return list("tenantId = ?1 and status = ?2 order by detectedAt desc",
                tenantId, status);
    }

    /**
     * Find breaches requiring DPB notification.
     * (Critical/High severity, not yet notified)
     *
     * @param tenantId Tenant identifier
     * @return List of breaches
     */
    public Uni<List<DataBreachLog>> findRequiringDpbNotification(String tenantId) {
        return list("tenantId = ?1 and severity in (?2, ?3) and dpbNotifiedAt is null " +
                        "order by detectedAt",
                tenantId,
                DataBreachLog.BreachSeverity.CRITICAL,
                DataBreachLog.BreachSeverity.HIGH);
    }

    public Uni<List<DataBreachLog>> findOverdueDpbNotifications(String tenantId, int hoursThreshold) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hoursThreshold);
        return list("tenantId = ?1 and severity in (?2, ?3) and dpbNotifiedAt is null " +
                        "and detectedAt < ?4 order by detectedAt",
                tenantId,
                DataBreachLog.BreachSeverity.CRITICAL,
                DataBreachLog.BreachSeverity.HIGH,
                threshold);
    }

    public Uni<List<DataBreachLog>> findUnresolved(String tenantId) {
        return list("tenantId = ?1 and status not in (?2, ?3) order by detectedAt",
                tenantId,
                DataBreachLog.BreachStatus.RESOLVED,
                DataBreachLog.BreachStatus.CLOSED);
    }

    public Uni<BreachStatistics> getStatistics(String tenantId) {
        return Uni.combine().all().unis(
                count("tenantId = ?1", tenantId),
                count("tenantId = ?1 and severity = ?2",
                        tenantId, DataBreachLog.BreachSeverity.CRITICAL),
                count("tenantId = ?1 and status = ?2",
                        tenantId, DataBreachLog.BreachStatus.RESOLVED),
                count("tenantId = ?1 and dpbNotifiedAt is null and severity in (?2, ?3)",
                        tenantId,
                        DataBreachLog.BreachSeverity.CRITICAL,
                        DataBreachLog.BreachSeverity.HIGH)
        ).combinedWith((total, critical, resolved, needingNotification) ->
                new BreachStatistics(
                        total,
                        critical,
                        resolved,
                        needingNotification
                )
        );
    }

    public Multi<DataBreachLog> streamByTenant(String tenantId) {
        return list("tenantId = ?1 order by detectedAt desc", tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    public record BreachStatistics(
            long totalBreaches,
            long criticalBreaches,
            long resolvedBreaches,
            long needingDpbNotification
    ) {}
}
