package com.hospital.common.rbac.service;

import com.hospital.common.config.TenantContext;
import com.hospital.common.rbac.entity.Permission;
import com.hospital.common.rbac.entity.Role;
import com.hospital.common.rbac.entity.UserRole;
import com.hospital.common.rbac.repository.PermissionRepository;
import com.hospital.common.rbac.repository.RoleRepository;
import com.hospital.common.rbac.repository.UserResourcePermissionRepository;
import com.hospital.common.rbac.repository.UserRoleRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for runtime permission checks and authorization.
 *
 * This service provides methods to check if a user has specific permissions
 * based on their roles and resource-level permissions.
 *
 * Permission Resolution Order:
 * 1. Check JWT token permissions (fast path - no DB query)
 * 2. Check resource-level permissions (database query)
 * 3. Check role-based permissions (database query)
 *
 * Usage Examples:
 * ```java
 * // Check if user can read patients
 * permissionService.hasPermission("patient:read")
 *     .subscribe().with(hasAccess -> {
 *         if (hasAccess) {
 *             // Allow operation
 *         }
 *     });
 *
 * // Check if user can access a specific patient
 * permissionService.canAccessResource("patient", patientId, "read")
 *     .subscribe().with(canAccess -> {
 *         if (canAccess) {
 *             // Allow operation
 *         }
 *     });
 * ```
 */
@ApplicationScoped
public class PermissionService {

    @Inject
    JsonWebToken jwt;

    @Inject
    UserRoleRepository userRoleRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    PermissionRepository permissionRepository;

    @Inject
    UserResourcePermissionRepository userResourcePermissionRepository;

    /**
     * Check if the current user has a specific permission.
     * First checks JWT claims (fast), then falls back to database lookup.
     *
     * @param permissionName Permission name (e.g., "patient:read")
     * @return true if user has the permission
     */
    public Uni<Boolean> hasPermission(String permissionName) {
        try {
            // Fast path: Check JWT token permissions
            List<String> jwtPermissions = jwt.getClaim("permissions");
            if (jwtPermissions != null && jwtPermissions.contains(permissionName)) {
                Log.debugf("Permission %s found in JWT for user %s", permissionName, jwt.getSubject());
                return Uni.createFrom().item(true);
            }

            // Slow path: Query database for role-based permissions
            UUID userId = UUID.fromString(jwt.getSubject());
            String tenantId = TenantContext.getCurrentTenantOrThrow();

            return getUserPermissions(userId, tenantId)
                    .map(permissions -> {
                        boolean hasPermission = permissions.contains(permissionName);
                        Log.debugf("Permission %s %s in database for user %s",
                                permissionName, hasPermission ? "found" : "not found", userId);
                        return hasPermission;
                    });

        } catch (Exception e) {
            Log.errorf("Error checking permission %s: %s", permissionName, e.getMessage());
            return Uni.createFrom().item(false);
        }
    }

    /**
     * Check if the current user has any of the specified permissions.
     *
     * @param permissionNames List of permission names
     * @return true if user has at least one permission
     */
    public Uni<Boolean> hasAnyPermission(String... permissionNames) {
        List<Uni<Boolean>> checks = new ArrayList<>();
        for (String permission : permissionNames) {
            checks.add(hasPermission(permission));
        }

        return Uni.combine().all().unis(checks)
                .combinedWith(results -> {
                    for (Object result : results) {
                        if ((Boolean) result) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    /**
     * Check if the current user has all specified permissions.
     *
     * @param permissionNames List of permission names
     * @return true if user has all permissions
     */
    public Uni<Boolean> hasAllPermissions(String... permissionNames) {
        List<Uni<Boolean>> checks = new ArrayList<>();
        for (String permission : permissionNames) {
            checks.add(hasPermission(permission));
        }

        return Uni.combine().all().unis(checks)
                .combinedWith(results -> {
                    for (Object result : results) {
                        if (!(Boolean) result) {
                            return false;
                        }
                    }
                    return true;
                });
    }

    /**
     * Check if the current user can access a specific resource.
     * This checks both role-based and resource-level permissions.
     *
     * @param resourceType Resource type (e.g., "patient", "medical_record")
     * @param resourceId   Resource ID
     * @param action       Action (e.g., "read", "write")
     * @return true if user can access the resource
     */
    public Uni<Boolean> canAccessResource(String resourceType, UUID resourceId, String action) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            String tenantId = TenantContext.getCurrentTenantOrThrow();

            String permissionName = resourceType + ":" + action;

            // First check if user has general permission for this resource type
            return hasPermission(permissionName)
                    .chain(hasGeneralPermission -> {
                        if (hasGeneralPermission) {
                            return Uni.createFrom().item(true);
                        }

                        // Check resource-level permission
                        return userResourcePermissionRepository
                                .hasPermission(userId, tenantId, resourceType, resourceId, action);
                    });

        } catch (Exception e) {
            Log.errorf("Error checking resource access for %s:%s - %s",
                    resourceType, resourceId, e.getMessage());
            return Uni.createFrom().item(false);
        }
    }

    /**
     * Get all resources of a specific type that the current user can access.
     *
     * @param resourceType Resource type
     * @param action       Action (e.g., "read")
     * @return List of accessible resource IDs
     */
    public Uni<List<UUID>> getAccessibleResources(String resourceType, String action) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            String tenantId = TenantContext.getCurrentTenantOrThrow();

            String permissionName = resourceType + ":" + action;

            // If user has general permission, they can access all resources
            return hasPermission(permissionName)
                    .chain(hasGeneralPermission -> {
                        if (hasGeneralPermission) {
                            // Return empty list to indicate "all resources"
                            // Calling code should interpret empty list as "no restriction"
                            return Uni.createFrom().item(new ArrayList<UUID>());
                        }

                        // Return specific resources user has access to
                        return userResourcePermissionRepository
                                .findAccessibleResources(userId, tenantId, resourceType, action);
                    });

        } catch (Exception e) {
            Log.errorf("Error getting accessible resources for %s: %s", resourceType, e.getMessage());
            return Uni.createFrom().item(new ArrayList<>());
        }
    }

    /**
     * Get all permissions for a specific user in the current tenant.
     * Combines role-based permissions.
     *
     * @param userId   User ID
     * @param tenantId Tenant identifier
     * @return Set of permission names
     */
    public Uni<Set<String>> getUserPermissions(UUID userId, String tenantId) {
        // Get user's roles
        return userRoleRepository.findValidByUserAndTenant(userId, tenantId)
                .chain(userRoles -> {
                    if (userRoles.isEmpty()) {
                        return Uni.createFrom().item(new HashSet<String>());
                    }

                    // Extract role IDs
                    List<UUID> roleIds = userRoles.stream()
                            .map(UserRole::getRoleId)
                            .toList();

                    // Get roles with permissions
                    return roleRepository.findByIdsAndTenant(tenantId, roleIds)
                            .map(roles -> {
                                Set<String> permissions = new HashSet<>();
                                for (Role role : roles) {
                                    for (Permission permission : role.getPermissions()) {
                                        permissions.add(permission.getName());
                                    }
                                }
                                return permissions;
                            });
                });
    }

    /**
     * Check if the current user has a specific role.
     *
     * @param roleName Role name (e.g., "DOCTOR", "ADMIN")
     * @return true if user has the role
     */
    public Uni<Boolean> hasRole(String roleName) {
        try {
            // Fast path: Check JWT token roles
            List<String> jwtRoles = jwt.getClaim("roles");
            if (jwtRoles != null && jwtRoles.contains(roleName)) {
                return Uni.createFrom().item(true);
            }

            // Slow path: Query database
            UUID userId = UUID.fromString(jwt.getSubject());
            String tenantId = TenantContext.getCurrentTenantOrThrow();

            return roleRepository.findByNameAndTenant(tenantId, roleName)
                    .chain(role -> {
                        if (role == null) {
                            return Uni.createFrom().item(false);
                        }
                        return userRoleRepository.hasRole(userId, role.getId(), tenantId);
                    });

        } catch (Exception e) {
            Log.errorf("Error checking role %s: %s", roleName, e.getMessage());
            return Uni.createFrom().item(false);
        }
    }

    /**
     * Check if the current user has any of the specified roles.
     *
     * @param roleNames List of role names
     * @return true if user has at least one role
     */
    public Uni<Boolean> hasAnyRole(String... roleNames) {
        List<Uni<Boolean>> checks = new ArrayList<>();
        for (String role : roleNames) {
            checks.add(hasRole(role));
        }

        return Uni.combine().all().unis(checks)
                .combinedWith(results -> {
                    for (Object result : results) {
                        if ((Boolean) result) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    /**
     * Get current user ID from JWT token.
     *
     * @return User ID
     */
    public UUID getCurrentUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    /**
     * Get current user email from JWT token.
     *
     * @return User email
     */
    public String getCurrentUserEmail() {
        return jwt.getClaim("email");
    }

    /**
     * Get current user name from JWT token.
     *
     * @return User name
     */
    public String getCurrentUserName() {
        return jwt.getClaim("name");
    }

    /**
     * Get current user's roles from JWT token.
     *
     * @return List of role names
     */
    public List<String> getCurrentUserRoles() {
        List<String> roles = jwt.getClaim("roles");
        return roles != null ? roles : new ArrayList<>();
    }

    /**
     * Get current user's department from JWT token.
     *
     * @return Department name or null
     */
    public String getCurrentUserDepartment() {
        return jwt.getClaim("department");
    }

    /**
     * Check if current user is an admin.
     *
     * @return true if user has ADMIN role
     */
    public Uni<Boolean> isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if current user is a doctor.
     *
     * @return true if user has DOCTOR role
     */
    public Uni<Boolean> isDoctor() {
        return hasRole("DOCTOR");
    }

    /**
     * Check if current user is a nurse.
     *
     * @return true if user has NURSE role
     */
    public Uni<Boolean> isNurse() {
        return hasRole("NURSE");
    }

    /**
     * Check if current user is a patient.
     *
     * @return true if user has PATIENT role
     */
    public Uni<Boolean> isPatient() {
        return hasRole("PATIENT");
    }

    /**
     * Require a specific permission or throw exception.
     *
     * @param permissionName Permission name
     * @return Void uni that fails if permission is missing
     */
    public Uni<Void> requirePermission(String permissionName) {
        return hasPermission(permissionName)
                .chain(hasPermission -> {
                    if (!hasPermission) {
                        return Uni.createFrom().failure(
                                new SecurityException("Permission denied: " + permissionName)
                        );
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Require a specific role or throw exception.
     *
     * @param roleName Role name
     * @return Void uni that fails if role is missing
     */
    public Uni<Void> requireRole(String roleName) {
        return hasRole(roleName)
                .chain(hasRole -> {
                    if (!hasRole) {
                        return Uni.createFrom().failure(
                                new SecurityException("Role required: " + roleName)
                        );
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
