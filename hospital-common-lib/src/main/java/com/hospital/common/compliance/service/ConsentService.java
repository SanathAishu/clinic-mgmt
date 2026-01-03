package com.hospital.common.compliance.service;

import com.hospital.common.compliance.entity.Consent;
import com.hospital.common.compliance.repository.ConsentRepository;
import com.hospital.common.config.TenantContext;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing patient consent in compliance with DPDPA 2023.
 *
 * DPDPA Requirements Implemented:
 * 1. Purpose-specific consent collection
 * 2. Ability to withdraw consent
 * 3. Consent expiration management
 * 4. Audit trail of all consent actions
 * 5. Right to be forgotten (withdraw all consents)
 *
 * Usage Examples:
 * ```java
 * // Grant consent for treatment
 * consentService.grantConsent(patientId, ConsentPurpose.TREATMENT,
 *     "Medical treatment and care", ConsentMethod.WEB_FORM, null, ipAddress, userAgent)
 *     .subscribe().with(consent -> Log.info("Consent granted"));
 *
 * // Check if patient has consent
 * consentService.hasValidConsent(patientId, ConsentPurpose.RESEARCH)
 *     .subscribe().with(hasConsent -> {
 *         if (hasConsent) {
 *             // Proceed with research data usage
 *         }
 *     });
 *
 * // Withdraw consent
 * consentService.withdrawConsent(consentId, "Patient request")
 *     .subscribe().with(withdrawn -> Log.info("Consent withdrawn"));
 * ```
 */
@ApplicationScoped
public class ConsentService {

    @Inject
    ConsentRepository consentRepository;

    /**
     * Grant consent for a patient.
     *
     * @param patientId       Patient ID
     * @param purpose         Consent purpose
     * @param description     Detailed description
     * @param method          How consent was obtained
     * @param expiresAt       Optional expiration date
     * @param ipAddress       IP address for audit
     * @param userAgent       User agent for audit
     * @param recordedBy      Staff member who recorded consent
     * @param consentVersion  Version of consent form
     * @return Created consent
     */
    public Uni<Consent> grantConsent(UUID patientId, Consent.ConsentPurpose purpose,
                                    String description, Consent.ConsentMethod method,
                                    LocalDateTime expiresAt, String ipAddress,
                                    String userAgent, UUID recordedBy, String consentVersion) {
        String tenantId = TenantContext.getCurrentTenantOrThrow();

        // Check if active consent already exists for this purpose
        return consentRepository.findActiveByPatientAndPurpose(patientId, tenantId, purpose)
                .chain(existingConsent -> {
                    if (existingConsent != null) {
                        // Supersede existing consent
                        existingConsent.setStatus(Consent.ConsentStatus.SUPERSEDED);
                        return consentRepository.persist(existingConsent);
                    }
                    return Uni.createFrom().nullItem();
                })
                .chain(() -> {
                    // Create new consent
                    Consent consent = new Consent();
                    consent.setTenantId(tenantId);
                    consent.setPatientId(patientId);
                    consent.setPurpose(purpose);
                    consent.setDescription(description);
                    consent.setStatus(Consent.ConsentStatus.ACTIVE);
                    consent.setConsentMethod(method);
                    consent.setIpAddress(ipAddress);
                    consent.setUserAgent(userAgent);
                    consent.setGrantedAt(LocalDateTime.now());
                    consent.setExpiresAt(expiresAt);
                    consent.setRecordedBy(recordedBy);
                    consent.setConsentVersion(consentVersion);

                    return consentRepository.persist(consent);
                })
                .invoke(consent ->
                        Log.infof("Consent granted: patient=%s, purpose=%s, tenant=%s",
                                patientId, purpose, tenantId)
                );
    }

    public Uni<Consent> withdrawConsent(UUID consentId, String reason) {
        return consentRepository.findById(consentId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("Consent", consentId.toString())
                )
                .chain(consent -> {
                    if (consent.getStatus() != Consent.ConsentStatus.ACTIVE) {
                        return Uni.createFrom().failure(
                                new ValidationException("status",
                                        "Can only withdraw active consents")
                        );
                    }

                    consent.withdraw(reason);
                    return consentRepository.persist(consent);
                })
                .invoke(consent ->
                        Log.infof("Consent withdrawn: id=%s, reason=%s", consentId, reason)
                );
    }

    public Uni<Integer> withdrawAllConsents(UUID patientId, String reason) {
        String tenantId = TenantContext.getCurrentTenantOrThrow();

        return consentRepository.withdrawAllForPatient(patientId, tenantId, reason)
                .invoke(count ->
                        Log.infof("All consents withdrawn for patient: id=%s, count=%d, tenant=%s",
                                patientId, count, tenantId)
                );
    }

    public Uni<Boolean> hasValidConsent(UUID patientId, Consent.ConsentPurpose purpose) {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return consentRepository.hasValidConsent(patientId, tenantId, purpose);
    }

    public Uni<Void> requireConsent(UUID patientId, Consent.ConsentPurpose purpose) {
        return hasValidConsent(patientId, purpose)
                .chain(hasConsent -> {
                    if (!hasConsent) {
                        return Uni.createFrom().failure(
                                new SecurityException(
                                        String.format("Patient consent required for %s but not granted", purpose)
                                )
                        );
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    public Uni<List<Consent>> getActiveConsents(UUID patientId) {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return consentRepository.findActiveByPatient(patientId, tenantId);
    }

    public Uni<List<Consent>> getAllConsents(UUID patientId) {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return consentRepository.findAllByPatient(patientId, tenantId);
    }

    public Uni<Consent> getConsentById(UUID consentId) {
        return consentRepository.findById(consentId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("Consent", consentId.toString())
                );
    }

    public Uni<List<Consent>> findExpiringSoon(int days) {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return consentRepository.findExpiringSoon(tenantId, days);
    }

    /**
     * Renew an expiring consent.
     *
     * @param consentId      Original consent ID
     * @param newExpiresAt   New expiration date
     * @param method         How renewal was obtained
     * @param ipAddress      IP address for audit
     * @param userAgent      User agent for audit
     * @param recordedBy     Staff member who recorded renewal
     * @param consentVersion Version of consent form
     * @return New consent
     */
    public Uni<Consent> renewConsent(UUID consentId, LocalDateTime newExpiresAt,
                                    Consent.ConsentMethod method, String ipAddress,
                                    String userAgent, UUID recordedBy, String consentVersion) {
        return consentRepository.findById(consentId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("Consent", consentId.toString())
                )
                .chain(oldConsent -> {
                    // Supersede old consent
                    oldConsent.setStatus(Consent.ConsentStatus.SUPERSEDED);

                    // Create renewed consent
                    Consent newConsent = new Consent();
                    newConsent.setTenantId(oldConsent.getTenantId());
                    newConsent.setPatientId(oldConsent.getPatientId());
                    newConsent.setPurpose(oldConsent.getPurpose());
                    newConsent.setDescription(oldConsent.getDescription());
                    newConsent.setStatus(Consent.ConsentStatus.ACTIVE);
                    newConsent.setConsentMethod(method);
                    newConsent.setIpAddress(ipAddress);
                    newConsent.setUserAgent(userAgent);
                    newConsent.setGrantedAt(LocalDateTime.now());
                    newConsent.setExpiresAt(newExpiresAt);
                    newConsent.setRecordedBy(recordedBy);
                    newConsent.setParentConsentId(oldConsent.getId());
                    newConsent.setConsentVersion(consentVersion);

                    return consentRepository.persist(oldConsent)
                            .chain(() -> consentRepository.persist(newConsent));
                })
                .invoke(consent ->
                        Log.infof("Consent renewed: original=%s, new=%s", consentId, consent.getId())
                );
    }

    public Uni<Integer> markExpiredConsents() {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return consentRepository.markExpiredConsents(tenantId)
                .invoke(count ->
                        Log.infof("Marked %d consents as expired for tenant %s", count, tenantId)
                );
    }

    public Uni<ConsentRepository.ConsentStatistics> getStatistics() {
        String tenantId = TenantContext.getCurrentTenantOrThrow();
        return consentRepository.getStatistics(tenantId);
    }

    /**
     * Validate consent before processing sensitive data.
     * This is a helper method to enforce DPDPA compliance.
     *
     * @param patientId Patient ID
     * @param purpose   Consent purpose
     * @param operation Description of the operation being performed
     * @return Void uni that fails if consent is missing
     */
    public Uni<Void> validateConsentForOperation(UUID patientId, Consent.ConsentPurpose purpose,
                                                 String operation) {
        return hasValidConsent(patientId, purpose)
                .chain(hasConsent -> {
                    if (!hasConsent) {
                        Log.warnf("Consent validation failed: patient=%s, purpose=%s, operation=%s",
                                patientId, purpose, operation);

                        return Uni.createFrom().failure(
                                new SecurityException(
                                        String.format("Cannot perform '%s': Patient consent for %s not granted",
                                                operation, purpose)
                                )
                        );
                    }

                    Log.debugf("Consent validated: patient=%s, purpose=%s, operation=%s",
                            patientId, purpose, operation);
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Grant default consents for a new patient.
     * Typically called during patient registration.
     *
     * @param patientId      Patient ID
     * @param method         How consent was obtained
     * @param ipAddress      IP address
     * @param userAgent      User agent
     * @param recordedBy     Staff member
     * @param consentVersion Consent form version
     * @return List of granted consents
     */
    public Uni<List<Consent>> grantDefaultConsents(UUID patientId, Consent.ConsentMethod method,
                                                   String ipAddress, String userAgent,
                                                   UUID recordedBy, String consentVersion) {
        // Grant default required consents
        return Uni.combine().all().unis(
                grantConsent(patientId, Consent.ConsentPurpose.TREATMENT,
                        "Consent for medical treatment and care",
                        method, null, ipAddress, userAgent, recordedBy, consentVersion),
                grantConsent(patientId, Consent.ConsentPurpose.DATA_PROCESSING,
                        "Consent for data storage and processing",
                        method, null, ipAddress, userAgent, recordedBy, consentVersion),
                grantConsent(patientId, Consent.ConsentPurpose.COMMUNICATION,
                        "Consent for healthcare communications",
                        method, null, ipAddress, userAgent, recordedBy, consentVersion)
        ).combinedWith((treatment, dataProcessing, communication) ->
                List.of(treatment, dataProcessing, communication)
        );
    }
}
