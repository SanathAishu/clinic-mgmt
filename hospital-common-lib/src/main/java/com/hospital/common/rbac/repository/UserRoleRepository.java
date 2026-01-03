package com.hospital.common.rbac.repository;

import com.hospital.common.rbac.entity.UserRole;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for UserRole entity with tenant-aware queries.
 */
@ApplicationScoped
public class UserRoleRepository implements PanacheRepositoryBase<UserRole, UUID> {

    /**
     * Find all roles for a user in a specific tenant.
     *
     * @param userId   User ID
     * @param tenantId Tenant identifier
     * @return List of user roles
     */
    public Uni<List<UserRole>> findByUserAndTenant(UUID userId, String tenantId) {
        return list("userId = ?1 and tenantId = ?2 and active = true", userId, tenantId);
    }

    /**
     * Find all active and valid roles for a user in a tenant.
     * This filters by validity period as well.
     *
     * @param userId   User ID
     * @param tenantId Tenant identifier
     * @return List of valid user roles
     */
    public Uni<List<UserRole>> findValidByUserAndTenant(UUID userId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return list(
                "userId = ?1 and tenantId = ?2 and active = true " +
                        "and (validFrom is null or validFrom <= ?3) " +
                        "and (validUntil is null or validUntil > ?3)",
                userId, tenantId, now
        );
    }

    /**
     * Stream all roles for a user in a tenant.
     *
     * @param userId   User ID
     * @param tenantId Tenant identifier
     * @return Stream of user roles
     */
    public Multi<UserRole> streamByUserAndTenant(UUID userId, String tenantId) {
        // In reactive Panache, convert list to Multi
        return list("userId = ?1 and tenantId = ?2 and active = true", userId, tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Find all users with a specific role in a tenant.
     *
     * @param roleId   Role ID
     * @param tenantId Tenant identifier
     * @return List of user roles
     */
    public Uni<List<UserRole>> findByRoleAndTenant(UUID roleId, String tenantId) {
        return list("roleId = ?1 and tenantId = ?2 and active = true", roleId, tenantId);
    }

    /**
     * Find users with a specific role in a department.
     *
     * @param roleId     Role ID
     * @param tenantId   Tenant identifier
     * @param department Department name
     * @return List of user roles
     */
    public Uni<List<UserRole>> findByRoleTenantAndDepartment(UUID roleId, String tenantId, String department) {
        return list("roleId = ?1 and tenantId = ?2 and department = ?3 and active = true",
                roleId, tenantId, department);
    }

    /**
     * Check if a user has a specific role in a tenant.
     *
     * @param userId   User ID
     * @param roleId   Role ID
     * @param tenantId Tenant identifier
     * @return true if user has the role
     */
    public Uni<Boolean> hasRole(UUID userId, UUID roleId, String tenantId) {
        return count("userId = ?1 and roleId = ?2 and tenantId = ?3 and active = true",
                userId, roleId, tenantId)
                .map(count -> count > 0);
    }

    /**
     * Assign a role to a user.
     *
     * @param userId     User ID
     * @param roleId     Role ID
     * @param tenantId   Tenant identifier
     * @param department Optional department
     * @param assignedBy User who assigned the role
     * @return Created UserRole
     */
    public Uni<UserRole> assignRole(UUID userId, UUID roleId, String tenantId, String department, UUID assignedBy) {
        // First check if assignment already exists
        return count("userId = ?1 and roleId = ?2 and tenantId = ?3 and active = true",
                userId, roleId, tenantId)
                .chain(count -> {
                    if (count > 0) {
                        // Already assigned
                        return findByUserRoleAndTenant(userId, roleId, tenantId);
                    } else {
                        // Create new assignment
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        userRole.setTenantId(tenantId);
                        userRole.setDepartment(department);
                        userRole.setAssignedBy(assignedBy);
                        userRole.setActive(true);
                        return persist(userRole);
                    }
                });
    }

    /**
     * Revoke a role from a user.
     *
     * @param userId   User ID
     * @param roleId   Role ID
     * @param tenantId Tenant identifier
     * @return Number of revoked assignments
     */
    public Uni<Integer> revokeRole(UUID userId, UUID roleId, String tenantId) {
        return update("active = false where userId = ?1 and roleId = ?2 and tenantId = ?3",
                userId, roleId, tenantId);
    }

    /**
     * Revoke all roles for a user in a tenant.
     *
     * @param userId   User ID
     * @param tenantId Tenant identifier
     * @return Number of revoked assignments
     */
    public Uni<Integer> revokeAllRoles(UUID userId, String tenantId) {
        return update("active = false where userId = ?1 and tenantId = ?2", userId, tenantId);
    }

    /**
     * Find a specific user-role assignment.
     *
     * @param userId   User ID
     * @param roleId   Role ID
     * @param tenantId Tenant identifier
     * @return UserRole if found
     */
    public Uni<UserRole> findByUserRoleAndTenant(UUID userId, UUID roleId, String tenantId) {
        return find("userId = ?1 and roleId = ?2 and tenantId = ?3 and active = true",
                userId, roleId, tenantId)
                .firstResult();
    }

    /**
     * Get count of users with a specific role.
     *
     * @param roleId   Role ID
     * @param tenantId Tenant identifier
     * @return Count of users
     */
    public Uni<Long> countUsersWithRole(UUID roleId, String tenantId) {
        return count("roleId = ?1 and tenantId = ?2 and active = true", roleId, tenantId);
    }
}
