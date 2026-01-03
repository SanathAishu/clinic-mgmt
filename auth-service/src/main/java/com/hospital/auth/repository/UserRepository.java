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

    /**
     * Find user by email within a tenant.
     * Used for login and email uniqueness checks.
     *
     * @param tenantId The tenant ID
     * @param email User's email
     * @return Uni with user or null if not found
     */
    public Uni<User> findByEmail(String tenantId, String email) {
        return find("tenantId = ?1 AND email = ?2", tenantId, email).firstResult();
    }

    /**
     * Check if email exists within a tenant.
     *
     * @param tenantId The tenant ID
     * @param email Email to check
     * @return Uni with true if exists, false otherwise
     */
    public Uni<Boolean> existsByEmail(String tenantId, String email) {
        return find("tenantId = ?1 AND email = ?2", tenantId, email)
            .count()
            .map(count -> count > 0);
    }

    /**
     * Find user by ID within a tenant (defense in depth).
     *
     * @param tenantId The tenant ID
     * @param userId User's UUID
     * @return Uni with user or null if not found
     */
    public Uni<User> findByIdAndTenant(String tenantId, UUID userId) {
        return find("tenantId = ?1 AND id = ?2", tenantId, userId).firstResult();
    }

    /**
     * Find all active users for a tenant.
     *
     * @param tenantId The tenant ID
     * @return Multi stream of active users
     */
    public Multi<User> findActiveUsers(String tenantId) {
        return list("tenantId = ?1 AND active = true ORDER BY createdAt DESC", tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Find all users for a tenant (paginated).
     *
     * @param tenantId The tenant ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Uni with list of users
     */
    public Uni<java.util.List<User>> findByTenantPaginated(String tenantId, int page, int size) {
        return find("tenantId = ?1 ORDER BY createdAt DESC", tenantId)
            .page(page, size)
            .list();
    }

    /**
     * Count total users for a tenant.
     *
     * @param tenantId The tenant ID
     * @return Uni with user count
     */
    public Uni<Long> countByTenant(String tenantId) {
        return count("tenantId = ?1", tenantId);
    }

    /**
     * Count active users for a tenant.
     *
     * @param tenantId The tenant ID
     * @return Uni with active user count
     */
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

    /**
     * Find users by email pattern (search).
     * Used for admin user search functionality.
     *
     * @param tenantId The tenant ID
     * @param emailPattern Email pattern (e.g., "%@hospital.com")
     * @return Multi stream of matching users
     */
    public Multi<User> searchByEmail(String tenantId, String emailPattern) {
        return list("tenantId = ?1 AND email LIKE ?2 ORDER BY email", tenantId, emailPattern)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Find users by name pattern (search).
     *
     * @param tenantId The tenant ID
     * @param namePattern Name pattern (e.g., "John%")
     * @return Multi stream of matching users
     */
    public Multi<User> searchByName(String tenantId, String namePattern) {
        return list("tenantId = ?1 AND LOWER(name) LIKE LOWER(?2) ORDER BY name", tenantId, namePattern)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Soft delete user (set active = false).
     * Preserves data for audit purposes.
     *
     * @param tenantId The tenant ID
     * @param userId User's UUID
     * @return Uni with number of updated rows
     */
    public Uni<Integer> softDelete(String tenantId, UUID userId) {
        return update("active = false WHERE tenantId = ?1 AND id = ?2", tenantId, userId);
    }

    /**
     * Reactivate user (set active = true).
     *
     * @param tenantId The tenant ID
     * @param userId User's UUID
     * @return Uni with number of updated rows
     */
    public Uni<Integer> reactivate(String tenantId, UUID userId) {
        return update("active = true, lockedUntil = null, failedLoginAttempts = 0 " +
                     "WHERE tenantId = ?1 AND id = ?2", tenantId, userId);
    }
}
