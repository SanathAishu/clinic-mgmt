package com.hospital.common.rbac.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Role entity for dynamic RBAC.
 *
 * Represents a role within a specific tenant. Roles are tenant-specific,
 * meaning "DOCTOR" role in Tenant A is separate from "DOCTOR" in Tenant B.
 *
 * Examples:
 * - Base roles: DOCTOR, NURSE, RECEPTIONIST, PATIENT, ADMIN
 * - Custom roles: DEPARTMENT_HEAD, EMERGENCY_STAFF, LAB_TECHNICIAN
 *
 * Roles can have multiple permissions attached via RolePermission mapping.
 */
@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Role extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /**
     * Built-in system roles cannot be deleted.
     * Examples: DOCTOR, NURSE, ADMIN, PATIENT, RECEPTIONIST
     */
    @Column(name = "is_system_role", nullable = false)
    private boolean isSystemRole = false;

    /**
     * Active status - inactive roles cannot be assigned to users.
     */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Permissions associated with this role.
     * Lazy loading to avoid N+1 query issues.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Add a permission to this role.
     *
     * @param permission Permission to add
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    /**
     * Remove a permission from this role.
     *
     * @param permission Permission to remove
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    /**
     * Check if this role has a specific permission.
     *
     * @param permissionName Permission name to check (e.g., "medical_record:read")
     * @return true if role has the permission
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream()
                .anyMatch(p -> p.getName().equals(permissionName));
    }
}
