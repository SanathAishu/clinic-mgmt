package com.hospital.common.rbac.repository;

import com.hospital.common.rbac.entity.Role;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Role entity with tenant-aware queries.
 */
@ApplicationScoped
public class RoleRepository implements PanacheRepositoryBase<Role, UUID> {

    /**
     * Find a role by name within a specific tenant.
     *
     * @param tenantId Tenant identifier
     * @param name     Role name
     * @return Role if found
     */
    public Uni<Role> findByNameAndTenant(String tenantId, String name) {
        return find("tenantId = ?1 and name = ?2", tenantId, name).firstResult();
    }

    /**
     * Find all active roles for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of active roles
     */
    public Uni<List<Role>> findActiveByTenant(String tenantId) {
        return list("tenantId = ?1 and active = true", tenantId);
    }

    /**
     * Find all roles for a tenant (including inactive).
     *
     * @param tenantId Tenant identifier
     * @return List of all roles
     */
    public Uni<List<Role>> findAllByTenant(String tenantId) {
        return list("tenantId", tenantId);
    }

    /**
     * Stream all active roles for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return Stream of active roles
     */
    public Multi<Role> streamActiveByTenant(String tenantId) {
        // In reactive Panache, convert list to Multi
        return list("tenantId = ?1 and active = true", tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Find all system roles (built-in roles).
     *
     * @param tenantId Tenant identifier
     * @return List of system roles
     */
    public Uni<List<Role>> findSystemRoles(String tenantId) {
        return list("tenantId = ?1 and isSystemRole = true", tenantId);
    }

    /**
     * Check if a role name exists in a tenant.
     *
     * @param tenantId Tenant identifier
     * @param name     Role name
     * @return true if exists
     */
    public Uni<Boolean> existsByNameAndTenant(String tenantId, String name) {
        return count("tenantId = ?1 and name = ?2", tenantId, name)
                .map(count -> count > 0);
    }

    /**
     * Find roles by IDs within a tenant.
     *
     * @param tenantId Tenant identifier
     * @param roleIds  List of role IDs
     * @return List of roles
     */
    public Uni<List<Role>> findByIdsAndTenant(String tenantId, List<UUID> roleIds) {
        return list("tenantId = ?1 and id in ?2", tenantId, roleIds);
    }

    /**
     * Deactivate a role (soft delete).
     *
     * @param roleId Role ID
     * @return Number of updated records
     */
    public Uni<Integer> deactivate(UUID roleId) {
        return update("active = false, updatedAt = current_timestamp where id = ?1", roleId);
    }

    /**
     * Activate a role.
     *
     * @param roleId Role ID
     * @return Number of updated records
     */
    public Uni<Integer> activate(UUID roleId) {
        return update("active = true, updatedAt = current_timestamp where id = ?1", roleId);
    }
}
