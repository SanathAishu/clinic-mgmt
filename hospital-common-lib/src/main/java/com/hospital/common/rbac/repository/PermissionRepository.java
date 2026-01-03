package com.hospital.common.rbac.repository;

import com.hospital.common.rbac.entity.Permission;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Permission entity.
 * Permissions are global (not tenant-specific).
 */
@ApplicationScoped
public class PermissionRepository implements PanacheRepositoryBase<Permission, UUID> {

    public Uni<Permission> findByName(String name) {
        return find("name", name).firstResult();
    }

    public Uni<List<Permission>> findByResource(String resource) {
        return list("resource", resource);
    }

    public Uni<List<Permission>> findByAction(String action) {
        return list("action", action);
    }

    public Uni<Permission> findByResourceAndAction(String resource, String action) {
        return find("resource = ?1 and action = ?2", resource, action).firstResult();
    }

    public Uni<Boolean> existsByName(String name) {
        return count("name", name).map(count -> count > 0);
    }

    public Uni<List<Permission>> findSystemPermissions() {
        return list("isSystemPermission", true);
    }

    public Multi<Permission> streamAll() {
        return streamAll();
    }

    public Uni<List<Permission>> findByIds(List<UUID> permissionIds) {
        return list("id in ?1", permissionIds);
    }

    public Uni<List<Permission>> findByNames(List<String> names) {
        return list("name in ?1", names);
    }

    public Uni<List<Permission>> searchByName(String pattern) {
        return list("name like ?1", pattern);
    }

    public Uni<List<String>> findAllResources() {
        return find("select distinct p.resource from Permission p")
                .project(String.class)
                .list();
    }

    public Uni<List<String>> findAllActions() {
        return find("select distinct p.action from Permission p")
                .project(String.class)
                .list();
    }
}
