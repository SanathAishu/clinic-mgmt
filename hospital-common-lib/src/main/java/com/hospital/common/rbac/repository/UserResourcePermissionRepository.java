package com.hospital.common.rbac.repository;

import com.hospital.common.rbac.entity.UserResourcePermission;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for UserResourcePermission entity.
 * Handles fine-grained resource-level permissions.
 */
@ApplicationScoped
public class UserResourcePermissionRepository implements PanacheRepositoryBase<UserResourcePermission, UUID> {

    /**
     * Find all permissions for a user on a specific resource.
     *
     * @param userId       User ID
     * @param tenantId     Tenant identifier
     * @param resourceType Resource type
     * @param resourceId   Resource ID
     * @return List of permissions
     */
    public Uni<List<UserResourcePermission>> findByUserAndResource(
            UUID userId, String tenantId, String resourceType, UUID resourceId) {
        return list("userId = ?1 and tenantId = ?2 and resourceType = ?3 and resourceId = ?4 and active = true",
                userId, tenantId, resourceType, resourceId);
    }

    /**
     * Find all valid permissions for a user on a specific resource.
     * Filters by validity period.
     *
     * @param userId       User ID
     * @param tenantId     Tenant identifier
     * @param resourceType Resource type
     * @param resourceId   Resource ID
     * @return List of valid permissions
     */
    public Uni<List<UserResourcePermission>> findValidByUserAndResource(
            UUID userId, String tenantId, String resourceType, UUID resourceId) {
        LocalDateTime now = LocalDateTime.now();
        return list(
                "userId = ?1 and tenantId = ?2 and resourceType = ?3 and resourceId = ?4 and active = true " +
                        "and revokedAt is null " +
                        "and (validFrom is null or validFrom <= ?5) " +
                        "and (validUntil is null or validUntil > ?5)",
                userId, tenantId, resourceType, resourceId, now
        );
    }

    /**
     * Check if a user has a specific permission on a resource.
     *
     * @param userId       User ID
     * @param tenantId     Tenant identifier
     * @param resourceType Resource type
     * @param resourceId   Resource ID
     * @param permission   Permission action
     * @return true if user has the permission
     */
    public Uni<Boolean> hasPermission(
            UUID userId, String tenantId, String resourceType, UUID resourceId, String permission) {
        LocalDateTime now = LocalDateTime.now();
        return count(
                "userId = ?1 and tenantId = ?2 and resourceType = ?3 and resourceId = ?4 " +
                        "and permission = ?5 and active = true and revokedAt is null " +
                        "and (validFrom is null or validFrom <= ?6) " +
                        "and (validUntil is null or validUntil > ?6)",
                userId, tenantId, resourceType, resourceId, permission, now
        ).map(count -> count > 0);
    }

    /**
     * Find all resources of a type that a user has access to.
     *
     * @param userId       User ID
     * @param tenantId     Tenant identifier
     * @param resourceType Resource type
     * @param permission   Permission action (optional, can be null for any permission)
     * @return List of resource IDs
     */
    public Uni<List<UUID>> findAccessibleResources(
            UUID userId, String tenantId, String resourceType, String permission) {
        LocalDateTime now = LocalDateTime.now();

        String query = "userId = ?1 and tenantId = ?2 and resourceType = ?3 and active = true " +
                "and revokedAt is null " +
                "and (validFrom is null or validFrom <= ?4) " +
                "and (validUntil is null or validUntil > ?4)";

        if (permission != null) {
            return list(query + " and permission = ?5", userId, tenantId, resourceType, now, permission)
                    .map(list -> list.stream()
                            .map(UserResourcePermission::getResourceId)
                            .distinct()
                            .toList());
        } else {
            return list(query, userId, tenantId, resourceType, now)
                    .map(list -> list.stream()
                            .map(UserResourcePermission::getResourceId)
                            .distinct()
                            .toList());
        }
    }

    /**
     * Grant a permission to a user for a specific resource.
     *
     * @param userId       User ID
     * @param tenantId     Tenant identifier
     * @param resourceType Resource type
     * @param resourceId   Resource ID
     * @param permission   Permission action
     * @param reason       Reason for granting
     * @param grantedBy    User who granted the permission
     * @param validUntil   Optional expiration time
     * @return Created permission
     */
    public Uni<UserResourcePermission> grantPermission(
            UUID userId, String tenantId, String resourceType, UUID resourceId,
            String permission, String reason, UUID grantedBy, LocalDateTime validUntil) {

        // Check if permission already exists
        return count("userId = ?1 and tenantId = ?2 and resourceType = ?3 and resourceId = ?4 " +
                        "and permission = ?5 and active = true and revokedAt is null",
                userId, tenantId, resourceType, resourceId, permission)
                .chain(count -> {
                    if (count > 0) {
                        // Already granted
                        return find("userId = ?1 and tenantId = ?2 and resourceType = ?3 and resourceId = ?4 " +
                                        "and permission = ?5 and active = true and revokedAt is null",
                                userId, tenantId, resourceType, resourceId, permission)
                                .firstResult();
                    } else {
                        // Create new permission
                        UserResourcePermission urp = new UserResourcePermission();
                        urp.setUserId(userId);
                        urp.setTenantId(tenantId);
                        urp.setResourceType(resourceType);
                        urp.setResourceId(resourceId);
                        urp.setPermission(permission);
                        urp.setReason(reason);
                        urp.setGrantedBy(grantedBy);
                        urp.setValidUntil(validUntil);
                        urp.setActive(true);
                        return persist(urp);
                    }
                });
    }

    /**
     * Grant break-glass emergency access.
     *
     * @param userId       User ID
     * @param tenantId     Tenant identifier
     * @param resourceType Resource type
     * @param resourceId   Resource ID
     * @param permission   Permission action
     * @param reason       Emergency reason
     * @param grantedBy    User who granted (can be same as userId for self-grant)
     * @param duration     Duration in minutes
     * @return Created permission
     */
    public Uni<UserResourcePermission> grantBreakGlassAccess(
            UUID userId, String tenantId, String resourceType, UUID resourceId,
            String permission, String reason, UUID grantedBy, int duration) {

        UserResourcePermission urp = new UserResourcePermission();
        urp.setUserId(userId);
        urp.setTenantId(tenantId);
        urp.setResourceType(resourceType);
        urp.setResourceId(resourceId);
        urp.setPermission(permission);
        urp.setReason("BREAK-GLASS: " + reason);
        urp.setGrantedBy(grantedBy);
        urp.setBreakGlass(true);
        urp.setValidUntil(LocalDateTime.now().plusMinutes(duration));
        urp.setActive(true);

        return persist(urp);
    }

    /**
     * Revoke a specific permission.
     *
     * @param permissionId Permission ID
     * @param revokedBy    User who revoked
     * @return Updated permission
     */
    public Uni<UserResourcePermission> revokePermission(UUID permissionId, UUID revokedBy) {
        return findById(permissionId)
                .chain(permission -> {
                    if (permission != null) {
                        permission.revoke(revokedBy);
                        return persist(permission);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    /**
     * Revoke all permissions for a user on a specific resource.
     *
     * @param userId       User ID
     * @param tenantId     Tenant identifier
     * @param resourceType Resource type
     * @param resourceId   Resource ID
     * @param revokedBy    User who revoked
     * @return Number of revoked permissions
     */
    public Uni<Integer> revokeAllForResource(
            UUID userId, String tenantId, String resourceType, UUID resourceId, UUID revokedBy) {
        LocalDateTime now = LocalDateTime.now();
        return update("active = false, revokedAt = ?1, revokedBy = ?2 " +
                        "where userId = ?3 and tenantId = ?4 and resourceType = ?5 and resourceId = ?6",
                now, revokedBy, userId, tenantId, resourceType, resourceId);
    }

    /**
     * Find all break-glass accesses for audit purposes.
     *
     * @param tenantId Tenant identifier
     * @return List of break-glass permissions
     */
    public Uni<List<UserResourcePermission>> findBreakGlassAccesses(String tenantId) {
        return list("tenantId = ?1 and isBreakGlass = true order by grantedAt desc", tenantId);
    }

    /**
     * Stream all permissions for a user.
     *
     * @param userId   User ID
     * @param tenantId Tenant identifier
     * @return Stream of permissions
     */
    public Multi<UserResourcePermission> streamByUser(UUID userId, String tenantId) {
        // In reactive Panache, convert list to Multi
        return list("userId = ?1 and tenantId = ?2 and active = true and revokedAt is null",
                userId, tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Cleanup expired permissions.
     *
     * @return Number of cleaned up permissions
     */
    public Uni<Integer> cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        return update("active = false where validUntil < ?1 and active = true", now);
    }
}
