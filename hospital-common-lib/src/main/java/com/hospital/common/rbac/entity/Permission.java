package com.hospital.common.rbac.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Permission entity for dynamic RBAC.
 *
 * Permissions are global (not tenant-specific) and define what actions
 * can be performed on resources.
 *
 * Permission Format: {resource}:{action}
 * Examples:
 * - patient:read
 * - patient:write
 * - patient:delete
 * - medical_record:read
 * - medical_record:write
 * - prescription:create
 * - appointment:cancel
 * - user:manage
 * - role:assign
 *
 * Permissions are assigned to roles via RolePermission mapping,
 * or directly to users for specific resources via UserResourcePermission.
 */
@Entity
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Permission extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Permission name in format: resource:action
     * Examples: "patient:read", "medical_record:write"
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Resource type this permission applies to.
     * Examples: patient, doctor, medical_record, prescription
     */
    @Column(nullable = false, length = 50)
    private String resource;

    /**
     * Action that can be performed.
     * Examples: read, write, delete, create, update, manage
     */
    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 255)
    private String description;

    /**
     * System permissions cannot be deleted.
     * These are core permissions required for application functionality.
     */
    @Column(name = "is_system_permission", nullable = false)
    private boolean isSystemPermission = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Auto-populate resource and action from name if not set
        if (name != null && (resource == null || action == null)) {
            String[] parts = name.split(":");
            if (parts.length == 2) {
                this.resource = parts[0];
                this.action = parts[1];
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Factory method to create a permission from resource and action.
     *
     * @param resource Resource type
     * @param action   Action type
     * @return Permission instance
     */
    public static Permission of(String resource, String action) {
        Permission permission = new Permission();
        permission.setResource(resource);
        permission.setAction(action);
        permission.setName(resource + ":" + action);
        return permission;
    }

    /**
     * Factory method to create a permission from name.
     *
     * @param name Permission name (e.g., "patient:read")
     * @return Permission instance
     */
    public static Permission of(String name) {
        Permission permission = new Permission();
        permission.setName(name);
        String[] parts = name.split(":");
        if (parts.length == 2) {
            permission.setResource(parts[0]);
            permission.setAction(parts[1]);
        }
        return permission;
    }

    /**
     * Check if this permission matches a given permission string.
     *
     * @param permissionString Permission string to check
     * @return true if matches
     */
    public boolean matches(String permissionString) {
        return this.name.equals(permissionString);
    }

    /**
     * Check if this permission allows a specific action on a resource.
     *
     * @param resource Resource to check
     * @param action   Action to check
     * @return true if matches
     */
    public boolean allows(String resource, String action) {
        return this.resource.equals(resource) && this.action.equals(action);
    }
}
