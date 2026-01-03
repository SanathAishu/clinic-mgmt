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

    public Uni<Role> findByNameAndTenant(String tenantId, String name) {
        return find("tenantId = ?1 and name = ?2", tenantId, name).firstResult();
    }

    public Uni<List<Role>> findActiveByTenant(String tenantId) {
        return list("tenantId = ?1 and active = true", tenantId);
    }

    public Uni<List<Role>> findAllByTenant(String tenantId) {
        return list("tenantId", tenantId);
    }

    public Multi<Role> streamActiveByTenant(String tenantId) {
        return list("tenantId = ?1 and active = true", tenantId)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    public Uni<List<Role>> findSystemRoles(String tenantId) {
        return list("tenantId = ?1 and isSystemRole = true", tenantId);
    }

    public Uni<Boolean> existsByNameAndTenant(String tenantId, String name) {
        return count("tenantId = ?1 and name = ?2", tenantId, name)
                .map(count -> count > 0);
    }

    public Uni<List<Role>> findByIdsAndTenant(String tenantId, List<UUID> roleIds) {
        return list("tenantId = ?1 and id in ?2", tenantId, roleIds);
    }

    // LEFT JOIN FETCH prevents N+1 queries when accessing permissions
    public Uni<List<Role>> findByIdsWithPermissions(String tenantId, List<UUID> roleIds) {
        return find(
            "SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions " +
            "WHERE r.tenantId = ?1 AND r.id IN ?2 AND r.active = true",
            tenantId, roleIds
        ).list();
    }

    public Uni<Integer> deactivate(UUID roleId) {
        return update("active = false, updatedAt = current_timestamp where id = ?1", roleId);
    }

    public Uni<Integer> activate(UUID roleId) {
        return update("active = true, updatedAt = current_timestamp where id = ?1", roleId);
    }
}
