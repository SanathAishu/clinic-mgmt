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
 * Consent entity for DPDPA (Digital Personal Data Protection Act) 2023 compliance.
 *
 * Tracks patient consent for various data processing purposes.
 * DPDPA requires:
 * - Purpose-specific consent
 * - Explicit consent collection
 * - Ability to withdraw consent
 * - Audit trail of all consent actions
 *
 * Consent Types:
 * - TREATMENT: Use data for medical treatment
 * - RESEARCH: Use data for medical research
 * - MARKETING: Use data for marketing communications
 * - DATA_SHARING: Share data with third parties
 * - EMERGENCY: Emergency access to medical records
 * - ANALYTICS: Use data for analytics and quality improvement
 */
@Entity
@Table(name = "consents", indexes = {
        @Index(name = "idx_consents_patient", columnList = "patient_id"),
        @Index(name = "idx_consents_tenant", columnList = "tenant_id"),
        @Index(name = "idx_consents_patient_tenant", columnList = "patient_id, tenant_id"),
        @Index(name = "idx_consents_purpose", columnList = "purpose"),
        @Index(name = "idx_consents_status", columnList = "status"),
        @Index(name = "idx_consents_expiry", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Consent extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ConsentPurpose purpose;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ConsentStatus status;

    @Column(name = "consent_method", length = 50)
    @Enumerated(EnumType.STRING)
    private ConsentMethod consentMethod;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "withdrawal_reason", length = 500)
    private String withdrawalReason;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "parent_consent_id")
    private UUID parentConsentId;

    @Column(name = "consent_version", length = 20)
    private String consentVersion;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ConsentStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        if (status != ConsentStatus.ACTIVE) {
            return false;
        }

        if (withdrawnAt != null) {
            return false;
        }

        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }

        return true;
    }

    public void withdraw(String reason) {
        this.status = ConsentStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
        this.withdrawalReason = reason;
    }

    public void expire() {
        this.status = ConsentStatus.EXPIRED;
    }

    public boolean isExpiringSoon(int days) {
        if (expiresAt == null) {
            return false;
        }

        LocalDateTime threshold = LocalDateTime.now().plusDays(days);
        return expiresAt.isBefore(threshold) && expiresAt.isAfter(LocalDateTime.now());
    }

    public enum ConsentPurpose {
        /**
         * Consent for medical treatment and care
         */
        TREATMENT,

        /**
         * Consent for medical research studies
         */
        RESEARCH,

        /**
         * Consent for marketing communications
         */
        MARKETING,

        /**
         * Consent to share data with third parties
         */
        DATA_SHARING,

        /**
         * Consent for emergency access to medical records
         */
        EMERGENCY,

        /**
         * Consent for data analytics and quality improvement
         */
        ANALYTICS,

        /**
         * Consent for telemedicine services
         */
        TELEMEDICINE,

        /**
         * Consent for data storage and processing
         */
        DATA_PROCESSING,

        /**
         * Consent for communication via email/SMS
         */
        COMMUNICATION
    }

    public enum ConsentStatus {
        /**
         * Consent is currently active and valid
         */
        ACTIVE,

        /**
         * Consent has been withdrawn by the patient
         */
        WITHDRAWN,

        /**
         * Consent has expired
         */
        EXPIRED,

        /**
         * Consent is pending patient confirmation
         */
        PENDING,

        /**
         * Consent was denied by the patient
         */
        DENIED,

        /**
         * Consent has been superseded by a newer version
         */
        SUPERSEDED
    }

    public enum ConsentMethod {
        /**
         * Consent obtained via web form
         */
        WEB_FORM,

        /**
         * Consent obtained via mobile app
         */
        MOBILE_APP,

        /**
         * Consent obtained via written form (paper)
         */
        PAPER_FORM,

        /**
         * Verbal consent recorded by staff
         */
        VERBAL,

        /**
         * Consent obtained via email
         */
        EMAIL,

        /**
         * Consent obtained via SMS
         */
        SMS,

        /**
         * Consent obtained via phone call
         */
        PHONE,

        /**
         * Consent implied through actions (where legally permitted)
         */
        IMPLIED
    }
}
