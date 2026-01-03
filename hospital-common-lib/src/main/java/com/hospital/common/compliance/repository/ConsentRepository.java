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

    public Uni<List<Consent>> findActiveByPatient(UUID patientId, String tenantId) {
        return list("patientId = ?1 and tenantId = ?2 and status = ?3 and withdrawnAt is null",
                patientId, tenantId, Consent.ConsentStatus.ACTIVE);
    }

    public Uni<List<Consent>> findValidByPatient(UUID patientId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return list("patientId = ?1 and tenantId = ?2 and status = ?3 and withdrawnAt is null " +
                        "and (expiresAt is null or expiresAt > ?4)",
                patientId, tenantId, Consent.ConsentStatus.ACTIVE, now);
    }

    public Uni<Consent> findActiveByPatientAndPurpose(UUID patientId, String tenantId,
                                                      Consent.ConsentPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        return find("patientId = ?1 and tenantId = ?2 and purpose = ?3 and status = ?4 " +
                        "and withdrawnAt is null and (expiresAt is null or expiresAt > ?5) " +
                        "order by grantedAt desc",
                patientId, tenantId, purpose, Consent.ConsentStatus.ACTIVE, now)
                .firstResult();
    }

    public Uni<Boolean> hasValidConsent(UUID patientId, String tenantId,
                                        Consent.ConsentPurpose purpose) {
        return findActiveByPatientAndPurpose(patientId, tenantId, purpose)
                .map(consent -> consent != null);
    }

    public Uni<List<Consent>> findAllByPatient(UUID patientId, String tenantId) {
        return list("patientId = ?1 and tenantId = ?2 order by grantedAt desc",
                patientId, tenantId);
    }

    public Uni<List<Consent>> findByPurpose(String tenantId, Consent.ConsentPurpose purpose) {
        return list("tenantId = ?1 and purpose = ?2 order by grantedAt desc",
                tenantId, purpose);
    }

    public Uni<List<Consent>> findExpiringSoon(String tenantId, int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(days);

        return list("tenantId = ?1 and status = ?2 and expiresAt is not null " +
                        "and expiresAt > ?3 and expiresAt <= ?4 order by expiresAt",
                tenantId, Consent.ConsentStatus.ACTIVE, now, threshold);
    }

    public Uni<List<Consent>> findExpiredNotMarked(String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return list("tenantId = ?1 and status = ?2 and expiresAt < ?3",
                tenantId, Consent.ConsentStatus.ACTIVE, now);
    }

    public Uni<Integer> withdrawAllForPatient(UUID patientId, String tenantId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        return update("status = ?1, withdrawnAt = ?2, withdrawalReason = ?3 " +
                        "where patientId = ?4 and tenantId = ?5 and status = ?6",
                Consent.ConsentStatus.WITHDRAWN, now, reason,
                patientId, tenantId, Consent.ConsentStatus.ACTIVE);
    }

    public Uni<Integer> markExpiredConsents(String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return update("status = ?1 where tenantId = ?2 and status = ?3 and expiresAt < ?4",
                Consent.ConsentStatus.EXPIRED, tenantId, Consent.ConsentStatus.ACTIVE, now);
    }

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

    public Multi<Consent> streamByTenant(String tenantId) {
        return list("tenantId", tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    public record ConsentStatistics(
            long activeCount,
            long withdrawnCount,
            long expiredCount,
            long treatmentConsentCount,
            long researchConsentCount
    ) {}
}
