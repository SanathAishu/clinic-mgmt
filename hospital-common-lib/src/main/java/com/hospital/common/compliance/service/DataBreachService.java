package com.hospital.common.compliance.service;

import com.hospital.common.compliance.entity.DataBreachLog;
import com.hospital.common.compliance.repository.DataBreachLogRepository;
import com.hospital.common.config.TenantContext;
import com.hospital.common.exception.NotFoundException;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing data breaches in compliance with DPDPA 2023.
 *
 * DPDPA Section 8: Data Breach Notification Requirements
 * - Notify Data Protection Board without undue delay
 * - Notify affected individuals if breach likely to cause harm
 * - Maintain comprehensive records of all breaches
 *
 * Usage:
 * ```java
 * // Report a breach
 * dataBreachService.reportBreach(
 *     "Unauthorized access to medical records",
 *     "External attacker accessed patient database",
 *     BreachSeverity.CRITICAL,
 *     "Medical records (PII, PHI)",
 *     500,
 *     reporterId
 * ).subscribe().with(breach -> {
 *     // Breach logged, initiate incident response
 * });
 *
 * // Notify DPB
 * dataBreachService.notifyDataProtectionBoard(breachId, "DPB-2024-001")
 *     .subscribe().with(breach -> Log.info("DPB notified"));
 * ```
 */
@ApplicationScoped
public class DataBreachService {

    @Inject
    DataBreachLogRepository breachRepository;

    /**
     * Report a new data breach.
     *
     * @param title              Breach title/summary
     * @param description        Detailed description
     * @param severity           Severity level
     * @param dataTypeAffected   Type of data compromised
     * @param individualsAffected Number of individuals affected
     * @param reportedBy         User who reported the breach
     * @return Created breach log
     */
    public Uni<DataBreachLog> reportBreach(String title, String description,
                                          DataBreachLog.BreachSeverity severity,
                                          String dataTypeAffected, Integer individualsAffected,
                                          UUID reportedBy) {
        String tenantId = TenantContext.getCurrentTenantOrThrow();

        DataBreachLog breach = new DataBreachLog();
        breach.setTenantId(tenantId);
        breach.setIncidentId(generateIncidentId());
        breach.setTitle(title);
        breach.setDescription(description);
        breach.setSeverity(severity);
        breach.setStatus(DataBreachLog.BreachStatus.DETECTED);
        breach.setDataTypeAffected(dataTypeAffected);
        breach.setIndividualsAffected(individualsAffected);
        breach.setDetectedAt(LocalDateTime.now());
        breach.setReportedBy(reportedBy);

        return breachRepository.persist(breach)
                .invoke(b -> {
                    Log.errorf("DATA BREACH REPORTED: incident=%s, severity=%s, individuals=%d, tenant=%s",
                            b.getIncidentId(), severity, individualsAffected, tenantId);

                    // If critical, immediately flag for DPB notification
                    if (severity == DataBreachLog.BreachSeverity.CRITICAL ||
                            severity == DataBreachLog.BreachSeverity.HIGH) {
                        Log.errorf("CRITICAL BREACH - DPB notification required: %s", b.getIncidentId());
                    }
                });
    }

    /**
     * Update breach status.
     *
     * @param breachId  Breach ID
     * @param newStatus New status
     * @return Updated breach
     */
    public Uni<DataBreachLog> updateBreachStatus(UUID breachId, DataBreachLog.BreachStatus newStatus) {
        return breachRepository.findById(breachId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("DataBreach", breachId.toString())
                )
                .chain(breach -> {
                    breach.setStatus(newStatus);
                    return breachRepository.persist(breach);
                })
                .invoke(breach ->
                        Log.infof("Breach status updated: incident=%s, status=%s",
                                breach.getIncidentId(), newStatus)
                );
    }

    /**
     * Notify Data Protection Board about the breach.
     *
     * @param breachId     Breach ID
     * @param dpbReference Reference number from DPB
     * @return Updated breach
     */
    public Uni<DataBreachLog> notifyDataProtectionBoard(UUID breachId, String dpbReference) {
        return breachRepository.findById(breachId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("DataBreach", breachId.toString())
                )
                .chain(breach -> {
                    breach.setDpbNotifiedAt(LocalDateTime.now());
                    breach.setDpbReference(dpbReference);
                    breach.setStatus(DataBreachLog.BreachStatus.DPB_NOTIFIED);
                    return breachRepository.persist(breach);
                })
                .invoke(breach ->
                        Log.infof("DPB notified: incident=%s, reference=%s",
                                breach.getIncidentId(), dpbReference)
                );
    }

    /**
     * Notify affected individuals.
     *
     * @param breachId Breach ID
     * @return Updated breach
     */
    public Uni<DataBreachLog> notifyAffectedIndividuals(UUID breachId) {
        return breachRepository.findById(breachId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("DataBreach", breachId.toString())
                )
                .chain(breach -> {
                    breach.setIndividualsNotifiedAt(LocalDateTime.now());
                    breach.setStatus(DataBreachLog.BreachStatus.INDIVIDUALS_NOTIFIED);
                    return breachRepository.persist(breach);
                })
                .invoke(breach ->
                        Log.infof("Individuals notified: incident=%s, count=%d",
                                breach.getIncidentId(), breach.getIndividualsAffected())
                );
    }

    /**
     * Record containment actions taken.
     *
     * @param breachId          Breach ID
     * @param containmentActions Description of containment actions
     * @return Updated breach
     */
    public Uni<DataBreachLog> recordContainmentActions(UUID breachId, String containmentActions) {
        return breachRepository.findById(breachId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("DataBreach", breachId.toString())
                )
                .chain(breach -> {
                    breach.setContainmentActions(containmentActions);
                    breach.setStatus(DataBreachLog.BreachStatus.CONTAINED);
                    return breachRepository.persist(breach);
                })
                .invoke(breach ->
                        Log.infof("Containment actions recorded: incident=%s", breach.getIncidentId())
                );
    }

    /**
     * Record remediation steps taken.
     *
     * @param breachId          Breach ID
     * @param remediationSteps Description of remediation
     * @return Updated breach
     */
    public Uni<DataBreachLog> recordRemediationSteps(UUID breachId, String remediationSteps) {
        return breachRepository.findById(breachId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("DataBreach", breachId.toString())
                )
                .chain(breach -> {
                    breach.setRemediationSteps(remediationSteps);
                    breach.setStatus(DataBreachLog.BreachStatus.REMEDIATION_IN_PROGRESS);
                    return breachRepository.persist(breach);
                })
                .invoke(breach ->
                        Log.infof("Remediation steps recorded: incident=%s", breach.getIncidentId())
                );
    }

    /**
     * Mark breach as resolved.
     *
     * @param breachId Breach ID
     * @return Updated breach
     */
    public Uni<DataBreachLog> resolveBreachIncident(UUID breachId) {
        return breachRepository.findById(breachId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("DataBreach", breachId.toString())
                )
                .chain(breach -> {
                    breach.setResolvedAt(LocalDateTime.now());
                    breach.setStatus(DataBreachLog.BreachStatus.RESOLVED);
                    return breachRepository.persist(breach);
                })
                .invoke(breach ->
                        Log.infof("Breach resolved: incident=%s, duration=%d hours",
                                breach.getIncidentId(),
                                java.time.Duration.between(breach.getDetectedAt(),
                                        breach.getResolvedAt()).toHours())
                );
    }

    /**
     * Get breaches requiring DPB notification.
     *
     * @return List of breaches needing notification
     */
    public Uni<List<DataBreachLog>> getBreachesRequiringDpbNotification() {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return breachRepository.findRequiringDpbNotification(tenantId);
    }

    /**
     * Get overdue DPB notifications.
     * DPDPA requires notification "without undue delay" - typically within 72 hours.
     *
     * @param hoursThreshold Hours after detection (default: 72)
     * @return List of overdue breaches
     */
    public Uni<List<DataBreachLog>> getOverdueDpbNotifications(int hoursThreshold) {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return breachRepository.findOverdueDpbNotifications(tenantId, hoursThreshold);
    }

    /**
     * Get unresolved breaches.
     *
     * @return List of unresolved breaches
     */
    public Uni<List<DataBreachLog>> getUnresolvedBreaches() {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return breachRepository.findUnresolved(tenantId);
    }

    /**
     * Get breach by incident ID.
     *
     * @param incidentId Incident identifier
     * @return Data breach log
     */
    public Uni<DataBreachLog> getBreachByIncidentId(String incidentId) {
        return breachRepository.findByIncidentId(incidentId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("DataBreach with incidentId", incidentId)
                );
    }

    /**
     * Get breach by ID.
     *
     * @param breachId Breach ID
     * @return Data breach log
     */
    public Uni<DataBreachLog> getBreachById(UUID breachId) {
        return breachRepository.findById(breachId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("DataBreach", breachId.toString())
                );
    }

    /**
     * Get all breaches for current tenant.
     *
     * @return List of breaches
     */
    public Uni<List<DataBreachLog>> getAllBreaches() {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return breachRepository.findByTenant(tenantId);
    }

    /**
     * Get breaches by severity.
     *
     * @param severity Severity level
     * @return List of breaches
     */
    public Uni<List<DataBreachLog>> getBreachesBySeverity(DataBreachLog.BreachSeverity severity) {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return breachRepository.findBySeverity(tenantId, severity);
    }

    /**
     * Get breach statistics.
     *
     * @return Statistics
     */
    public Uni<DataBreachLogRepository.BreachStatistics> getStatistics() {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return breachRepository.getStatistics(tenantId);
    }

    /**
     * Generate unique incident ID.
     *
     * @return Incident ID (format: BR-YYYYMMDD-UUID)
     */
    private String generateIncidentId() {
        String datePart = LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
        );
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("BR-%s-%s", datePart, uuidPart);
    }

    /**
     * Check for overdue notifications and log warnings.
     * This should be called periodically (e.g., every hour).
     *
     * @return Number of overdue notifications
     */
    public Uni<Long> checkAndWarnOverdueNotifications() {
        return getOverdueDpbNotifications(72) // 72 hours = 3 days
                .invoke(overdueBreaches -> {
                    if (!overdueBreaches.isEmpty()) {
                        Log.errorf("WARNING: %d data breach(es) have overdue DPB notifications",
                                overdueBreaches.size());
                        for (DataBreachLog breach : overdueBreaches) {
                            Log.errorf("OVERDUE: incident=%s, severity=%s, detected=%s",
                                    breach.getIncidentId(),
                                    breach.getSeverity(),
                                    breach.getDetectedAt());
                        }
                    }
                })
                .map(List::size)
                .map(Integer::longValue);
    }
}
