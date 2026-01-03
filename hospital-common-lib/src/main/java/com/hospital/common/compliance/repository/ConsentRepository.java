package com.hospital.common.compliance.repository;

import com.hospital.common.compliance.entity.Consent;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Consent entity with tenant-aware queries.
 */
@ApplicationScoped
public class ConsentRepository implements PanacheRepositoryBase<Consent, UUID> {

    /**
     * Find all active consents for a patient.
     *
     * @param patientId Patient ID
     * @param tenantId  Tenant identifier
     * @return List of active consents
     */
    public Uni<List<Consent>> findActiveByPatient(UUID patientId, String tenantId) {
        return list("patientId = ?1 and tenantId = ?2 and status = ?3 and withdrawnAt is null",
                patientId, tenantId, Consent.ConsentStatus.ACTIVE);
    }

    /**
     * Find all valid (active and not expired) consents for a patient.
     *
     * @param patientId Patient ID
     * @param tenantId  Tenant identifier
     * @return List of valid consents
     */
    public Uni<List<Consent>> findValidByPatient(UUID patientId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return list("patientId = ?1 and tenantId = ?2 and status = ?3 and withdrawnAt is null " +
                        "and (expiresAt is null or expiresAt > ?4)",
                patientId, tenantId, Consent.ConsentStatus.ACTIVE, now);
    }

    /**
     * Find active consent for a specific purpose.
     *
     * @param patientId Patient ID
     * @param tenantId  Tenant identifier
     * @param purpose   Consent purpose
     * @return Consent if found
     */
    public Uni<Consent> findActiveByPatientAndPurpose(UUID patientId, String tenantId,
                                                      Consent.ConsentPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        return find("patientId = ?1 and tenantId = ?2 and purpose = ?3 and status = ?4 " +
                        "and withdrawnAt is null and (expiresAt is null or expiresAt > ?5) " +
                        "order by grantedAt desc",
                patientId, tenantId, purpose, Consent.ConsentStatus.ACTIVE, now)
                .firstResult();
    }

    /**
     * Check if patient has valid consent for a purpose.
     *
     * @param patientId Patient ID
     * @param tenantId  Tenant identifier
     * @param purpose   Consent purpose
     * @return true if valid consent exists
     */
    public Uni<Boolean> hasValidConsent(UUID patientId, String tenantId,
                                        Consent.ConsentPurpose purpose) {
        return findActiveByPatientAndPurpose(patientId, tenantId, purpose)
                .map(consent -> consent != null);
    }

    /**
     * Find all consents for a patient (including withdrawn and expired).
     *
     * @param patientId Patient ID
     * @param tenantId  Tenant identifier
     * @return List of all consents
     */
    public Uni<List<Consent>> findAllByPatient(UUID patientId, String tenantId) {
        return list("patientId = ?1 and tenantId = ?2 order by grantedAt desc",
                patientId, tenantId);
    }

    /**
     * Find consents by purpose across all patients.
     *
     * @param tenantId Tenant identifier
     * @param purpose  Consent purpose
     * @return List of consents
     */
    public Uni<List<Consent>> findByPurpose(String tenantId, Consent.ConsentPurpose purpose) {
        return list("tenantId = ?1 and purpose = ?2 order by grantedAt desc",
                tenantId, purpose);
    }

    /**
     * Find consents expiring soon.
     *
     * @param tenantId Tenant identifier
     * @param days     Number of days threshold
     * @return List of expiring consents
     */
    public Uni<List<Consent>> findExpiringSoon(String tenantId, int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(days);

        return list("tenantId = ?1 and status = ?2 and expiresAt is not null " +
                        "and expiresAt > ?3 and expiresAt <= ?4 order by expiresAt",
                tenantId, Consent.ConsentStatus.ACTIVE, now, threshold);
    }

    /**
     * Find expired consents that need status update.
     *
     * @param tenantId Tenant identifier
     * @return List of expired consents still marked as active
     */
    public Uni<List<Consent>> findExpiredNotMarked(String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return list("tenantId = ?1 and status = ?2 and expiresAt < ?3",
                tenantId, Consent.ConsentStatus.ACTIVE, now);
    }

    /**
     * Withdraw all consents for a patient (right to be forgotten).
     *
     * @param patientId Patient ID
     * @param tenantId  Tenant identifier
     * @param reason    Reason for withdrawal
     * @return Number of consents withdrawn
     */
    public Uni<Integer> withdrawAllForPatient(UUID patientId, String tenantId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        return update("status = ?1, withdrawnAt = ?2, withdrawalReason = ?3 " +
                        "where patientId = ?4 and tenantId = ?5 and status = ?6",
                Consent.ConsentStatus.WITHDRAWN, now, reason,
                patientId, tenantId, Consent.ConsentStatus.ACTIVE);
    }

    /**
     * Mark expired consents as expired.
     *
     * @param tenantId Tenant identifier
     * @return Number of consents updated
     */
    public Uni<Integer> markExpiredConsents(String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return update("status = ?1 where tenantId = ?2 and status = ?3 and expiresAt < ?4",
                Consent.ConsentStatus.EXPIRED, tenantId, Consent.ConsentStatus.ACTIVE, now);
    }

    /**
     * Get consent statistics for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return Statistics map
     */
    public Uni<ConsentStatistics> getStatistics(String tenantId) {
        return Uni.combine().all().unis(
                count("tenantId = ?1 and status = ?2", tenantId, Consent.ConsentStatus.ACTIVE),
                count("tenantId = ?1 and status = ?2", tenantId, Consent.ConsentStatus.WITHDRAWN),
                count("tenantId = ?1 and status = ?2", tenantId, Consent.ConsentStatus.EXPIRED),
                count("tenantId = ?1 and purpose = ?2 and status = ?3",
                        tenantId, Consent.ConsentPurpose.TREATMENT, Consent.ConsentStatus.ACTIVE),
                count("tenantId = ?1 and purpose = ?2 and status = ?3",
                        tenantId, Consent.ConsentPurpose.RESEARCH, Consent.ConsentStatus.ACTIVE)
        ).combinedWith((active, withdrawn, expired, treatment, research) ->
                new ConsentStatistics(
                        active,
                        withdrawn,
                        expired,
                        treatment,
                        research
                )
        );
    }

    /**
     * Stream all consents for a tenant (for bulk operations).
     *
     * @param tenantId Tenant identifier
     * @return Stream of consents
     */
    public Multi<Consent> streamByTenant(String tenantId) {
        return list("tenantId", tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Consent statistics data class
     */
    public record ConsentStatistics(
            long activeCount,
            long withdrawnCount,
            long expiredCount,
            long treatmentConsentCount,
            long researchConsentCount
    ) {}
}
