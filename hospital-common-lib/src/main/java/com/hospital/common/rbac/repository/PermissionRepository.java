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

    /**
     * Find a permission by name.
     *
     * @param name Permission name (e.g., "patient:read")
     * @return Permission if found
     */
    public Uni<Permission> findByName(String name) {
        return find("name", name).firstResult();
    }

    /**
     * Find permissions by resource type.
     *
     * @param resource Resource type (e.g., "patient", "medical_record")
     * @return List of permissions
     */
    public Uni<List<Permission>> findByResource(String resource) {
        return list("resource", resource);
    }

    /**
     * Find permissions by action.
     *
     * @param action Action type (e.g., "read", "write")
     * @return List of permissions
     */
    public Uni<List<Permission>> findByAction(String action) {
        return list("action", action);
    }

    /**
     * Find permissions by resource and action.
     *
     * @param resource Resource type
     * @param action   Action type
     * @return Permission if found
     */
    public Uni<Permission> findByResourceAndAction(String resource, String action) {
        return find("resource = ?1 and action = ?2", resource, action).firstResult();
    }

    /**
     * Check if a permission name exists.
     *
     * @param name Permission name
     * @return true if exists
     */
    public Uni<Boolean> existsByName(String name) {
        return count("name", name).map(count -> count > 0);
    }

    /**
     * Find all system permissions (built-in).
     *
     * @return List of system permissions
     */
    public Uni<List<Permission>> findSystemPermissions() {
        return list("isSystemPermission", true);
    }

    /**
     * Stream all permissions.
     *
     * @return Stream of permissions
     */
    public Multi<Permission> streamAll() {
        return streamAll();
    }

    /**
     * Find permissions by IDs.
     *
     * @param permissionIds List of permission IDs
     * @return List of permissions
     */
    public Uni<List<Permission>> findByIds(List<UUID> permissionIds) {
        return list("id in ?1", permissionIds);
    }

    /**
     * Find permissions by names.
     *
     * @param names List of permission names
     * @return List of permissions
     */
    public Uni<List<Permission>> findByNames(List<String> names) {
        return list("name in ?1", names);
    }

    /**
     * Search permissions by name pattern.
     *
     * @param pattern Search pattern (e.g., "%patient%")
     * @return List of matching permissions
     */
    public Uni<List<Permission>> searchByName(String pattern) {
        return list("name like ?1", pattern);
    }

    /**
     * Get all unique resource types.
     *
     * @return List of resource types
     */
    public Uni<List<String>> findAllResources() {
        return find("select distinct p.resource from Permission p")
                .project(String.class)
                .list();
    }

    /**
     * Get all unique action types.
     *
     * @return List of action types
     */
    public Uni<List<String>> findAllActions() {
        return find("select distinct p.action from Permission p")
                .project(String.class)
                .list();
    }
}
