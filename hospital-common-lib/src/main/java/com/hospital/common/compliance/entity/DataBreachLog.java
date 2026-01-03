package com.hospital.common.compliance.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Breach Log entity for DPDPA compliance.
 *
 * DPDPA 2023 Section 8: Data breach notification requirements
 * - Notify Data Protection Board (DPB) without undue delay
 * - Notify affected individuals if breach likely to cause harm
 * - Maintain records of all data breaches
 *
 * Breach Severity Levels:
 * - CRITICAL: Immediate risk to individuals (PII exposure, medical records)
 * - HIGH: Significant risk requiring immediate action
 * - MEDIUM: Moderate risk requiring investigation
 * - LOW: Minor incident with minimal risk
 */
@Entity
@Table(name = "data_breach_logs", indexes = {
        @Index(name = "idx_breach_tenant", columnList = "tenant_id"),
        @Index(name = "idx_breach_severity", columnList = "severity"),
        @Index(name = "idx_breach_status", columnList = "status"),
        @Index(name = "idx_breach_detected", columnList = "detected_at"),
        @Index(name = "idx_breach_notified", columnList = "dpb_notified_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DataBreachLog extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /**
     * Incident ID for tracking
     */
    @Column(name = "incident_id", unique = true, length = 50)
    private String incidentId;

    /**
     * Title/summary of the breach
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Detailed description of the breach
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Severity level
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BreachSeverity severity;

    /**
     * Current status
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BreachStatus status;

    /**
     * Type of data affected
     */
    @Column(name = "data_type_affected", length = 100)
    private String dataTypeAffected;

    /**
     * Number of individuals affected
     */
    @Column(name = "individuals_affected")
    private Integer individualsAffected;

    /**
     * When the breach was detected
     */
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    /**
     * When the breach occurred (may be before detection)
     */
    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    /**
     * How the breach was detected
     */
    @Column(name = "detection_method", length = 255)
    private String detectionMethod;

    /**
     * Root cause of the breach
     */
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    /**
     * When Data Protection Board was notified
     */
    @Column(name = "dpb_notified_at")
    private LocalDateTime dpbNotifiedAt;

    /**
     * Reference number from DPB notification
     */
    @Column(name = "dpb_reference", length = 100)
    private String dpbReference;

    /**
     * When affected individuals were notified
     */
    @Column(name = "individuals_notified_at")
    private LocalDateTime individualsNotifiedAt;

    /**
     * Containment actions taken
     */
    @Column(name = "containment_actions", columnDefinition = "TEXT")
    private String containmentActions;

    /**
     * Remediation steps taken
     */
    @Column(name = "remediation_steps", columnDefinition = "TEXT")
    private String remediationSteps;

    /**
     * When the breach was resolved
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * User who reported the breach
     */
    @Column(name = "reported_by")
    private UUID reportedBy;

    /**
     * User responsible for handling the breach
     */
    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BreachStatus.DETECTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if DPB notification is required and overdue.
     * DPDPA requires notification "without undue delay"
     *
     * @param hoursThreshold Hours after detection
     * @return true if notification is overdue
     */
    public boolean isDpbNotificationOverdue(int hoursThreshold) {
        if (dpbNotifiedAt != null) {
            return false; // Already notified
        }

        LocalDateTime threshold = detectedAt.plusHours(hoursThreshold);
        return LocalDateTime.now().isAfter(threshold);
    }

    /**
     * Breach severity levels
     */
    public enum BreachSeverity {
        CRITICAL,  // Immediate threat - PII/PHI exposed
        HIGH,      // Significant risk
        MEDIUM,    // Moderate risk
        LOW        // Minor incident
    }

    /**
     * Breach status workflow
     */
    public enum BreachStatus {
        DETECTED,              // Breach detected, investigation started
        UNDER_INVESTIGATION,   // Actively investigating
        CONTAINED,             // Breach contained, no further spread
        DPB_NOTIFIED,          // Data Protection Board notified
        INDIVIDUALS_NOTIFIED,  // Affected individuals notified
        REMEDIATION_IN_PROGRESS, // Fixing the breach
        RESOLVED,              // Breach fully resolved
        CLOSED                 // Incident closed after review
    }
}
