package com.hospital.auth.repository;

import com.hospital.auth.entity.User;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Reactive repository for User entity with tenant-aware queries.
 *
 * CRITICAL: ALL queries MUST filter by tenantId for tenant isolation.
 *
 * Pattern:
 * - All finder methods accept tenantId as first parameter
 * - Use Panache query methods (find, list, stream, count)
 * - Return Uni<T> for single results, Multi<T> for streams
 */
@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    public Uni<User> findByEmail(String tenantId, String email) {
        return find("tenantId = ?1 AND email = ?2", tenantId, email).firstResult();
    }

    public Uni<Boolean> existsByEmail(String tenantId, String email) {
        return find("tenantId = ?1 AND email = ?2", tenantId, email)
            .count()
            .map(count -> count > 0);
    }

    public Uni<User> findByIdAndTenant(String tenantId, UUID userId) {
        return find("tenantId = ?1 AND id = ?2", tenantId, userId).firstResult();
    }

    public Multi<User> findActiveUsers(String tenantId) {
        return list("tenantId = ?1 AND active = true ORDER BY createdAt DESC", tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    public Uni<java.util.List<User>> findByTenantPaginated(String tenantId, int page, int size) {
        return find("tenantId = ?1 ORDER BY createdAt DESC", tenantId)
            .page(page, size)
            .list();
    }

    public Uni<Long> countByTenant(String tenantId) {
        return count("tenantId = ?1", tenantId);
    }

    public Uni<Long> countActiveByTenant(String tenantId) {
        return count("tenantId = ?1 AND active = true", tenantId);
    }

    /**
     * Find users with failed login attempts above threshold.
     * Used for security monitoring.
     *
     * @param tenantId The tenant ID
     * @param threshold Failed attempts threshold
     * @return Multi stream of users
     */
    public Multi<User> findUsersWithFailedLogins(String tenantId, int threshold) {
        return list("tenantId = ?1 AND failedLoginAttempts >= ?2", tenantId, threshold)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Find locked users for a tenant.
     * Used for security monitoring.
     *
     * @param tenantId The tenant ID
     * @return Multi stream of locked users
     */
    public Multi<User> findLockedUsers(String tenantId) {
        return list("tenantId = ?1 AND lockedUntil IS NOT NULL AND lockedUntil > CURRENT_TIMESTAMP", tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    public Multi<User> searchByEmail(String tenantId, String emailPattern) {
        return list("tenantId = ?1 AND email LIKE ?2 ORDER BY email", tenantId, emailPattern)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    public Multi<User> searchByName(String tenantId, String namePattern) {
        return list("tenantId = ?1 AND LOWER(name) LIKE LOWER(?2) ORDER BY name", tenantId, namePattern)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    public Uni<Integer> softDelete(String tenantId, UUID userId) {
        return update("active = false WHERE tenantId = ?1 AND id = ?2", tenantId, userId);
    }

    public Uni<Integer> reactivate(String tenantId, UUID userId) {
        return update("active = true, lockedUntil = null, failedLoginAttempts = 0 " +
                     "WHERE tenantId = ?1 AND id = ?2", tenantId, userId);
    }
}
