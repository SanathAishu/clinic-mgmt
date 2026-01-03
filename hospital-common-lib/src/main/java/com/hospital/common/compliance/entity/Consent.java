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

    /**
     * Patient who provided the consent
     */
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /**
     * Purpose for which consent is given.
     * Examples: TREATMENT, RESEARCH, MARKETING, DATA_SHARING, EMERGENCY, ANALYTICS
     */
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ConsentPurpose purpose;

    /**
     * Detailed description of what the consent covers
     */
    @Column(length = 1000)
    private String description;

    /**
     * Current status of the consent
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ConsentStatus status;

    /**
     * How the consent was obtained
     */
    @Column(name = "consent_method", length = 50)
    @Enumerated(EnumType.STRING)
    private ConsentMethod consentMethod;

    /**
     * IP address from which consent was given (for audit)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string (for audit)
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * When the consent was granted
     */
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    /**
     * When the consent expires (null = no expiration)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * When the consent was withdrawn (null = still active)
     */
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    /**
     * Reason for withdrawal
     */
    @Column(name = "withdrawal_reason", length = 500)
    private String withdrawalReason;

    /**
     * User who recorded the consent (staff member)
     */
    @Column(name = "recorded_by")
    private UUID recordedBy;

    /**
     * Parent consent ID if this is a renewal or modification
     */
    @Column(name = "parent_consent_id")
    private UUID parentConsentId;

    /**
     * Version of consent form/agreement
     */
    @Column(name = "consent_version", length = 20)
    private String consentVersion;

    /**
     * Notes or additional information
     */
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

    /**
     * Check if consent is currently valid.
     *
     * @return true if consent is active and not expired
     */
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

    /**
     * Withdraw this consent.
     *
     * @param reason Reason for withdrawal
     */
    public void withdraw(String reason) {
        this.status = ConsentStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
        this.withdrawalReason = reason;
    }

    /**
     * Mark consent as expired.
     */
    public void expire() {
        this.status = ConsentStatus.EXPIRED;
    }

    /**
     * Check if consent is about to expire (within days threshold).
     *
     * @param days Number of days threshold
     * @return true if expiring soon
     */
    public boolean isExpiringSoon(int days) {
        if (expiresAt == null) {
            return false;
        }

        LocalDateTime threshold = LocalDateTime.now().plusDays(days);
        return expiresAt.isBefore(threshold) && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Consent purpose enumeration
     */
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

    /**
     * Consent status enumeration
     */
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

    /**
     * Consent method enumeration - how consent was obtained
     */
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
