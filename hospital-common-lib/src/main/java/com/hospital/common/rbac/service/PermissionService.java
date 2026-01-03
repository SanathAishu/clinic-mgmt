package com.hospital.common.rbac.service;

import com.hospital.common.config.TenantContext;
import com.hospital.common.rbac.entity.Permission;
import com.hospital.common.rbac.entity.Role;
import com.hospital.common.rbac.entity.UserRole;
import com.hospital.common.rbac.repository.PermissionRepository;
import com.hospital.common.rbac.repository.RoleRepository;
import com.hospital.common.rbac.repository.UserResourcePermissionRepository;
import com.hospital.common.rbac.repository.UserRoleRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
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
 * RBAC permission and role checking with multi-tenant support.
 * Resolution order: JWT claims → resource-level perms → role-based perms
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

    public Uni<Boolean> hasPermission(String permissionName) {
        try {
            List<String> jwtPermissions = jwt.getClaim("permissions");
            if (jwtPermissions != null && jwtPermissions.contains(permissionName)) {
                return Uni.createFrom().item(true);
            }

            UUID userId = UUID.fromString(jwt.getSubject());
            String tenantId = TenantContext.getCurrentTenantOrThrow();

            return getUserPermissions(userId, tenantId)
                    .map(permissions -> permissions.contains(permissionName));

        } catch (Exception e) {
            Log.errorf("Error checking permission %s: %s", permissionName, e.getMessage());
            return Uni.createFrom().item(false);
        }
    }

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

    public Uni<Boolean> canAccessResource(String resourceType, UUID resourceId, String action) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            String tenantId = TenantContext.getCurrentTenantOrThrow();

            String permissionName = resourceType + ":" + action;

            return hasPermission(permissionName)
                    .chain(hasGeneralPermission -> {
                        if (hasGeneralPermission) {
                            return Uni.createFrom().item(true);
                        }

                        return userResourcePermissionRepository
                                .hasPermission(userId, tenantId, resourceType, resourceId, action);
                    });

        } catch (Exception e) {
            Log.errorf("Error checking resource access for %s:%s - %s",
                    resourceType, resourceId, e.getMessage());
            return Uni.createFrom().item(false);
        }
    }

    public Uni<List<UUID>> getAccessibleResources(String resourceType, String action) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            String tenantId = TenantContext.getCurrentTenantOrThrow();

            String permissionName = resourceType + ":" + action;

            return hasPermission(permissionName)
                    .chain(hasGeneralPermission -> {
                        if (hasGeneralPermission) {
                            return Uni.createFrom().item(new ArrayList<UUID>());
                        }

                        return userResourcePermissionRepository
                                .findAccessibleResources(userId, tenantId, resourceType, action);
                    });

        } catch (Exception e) {
            Log.errorf("Error getting accessible resources for %s: %s", resourceType, e.getMessage());
            return Uni.createFrom().item(new ArrayList<>());
        }
    }

    @WithSession
    public Uni<Set<String>> getUserPermissions(UUID userId, String tenantId) {
        return userRoleRepository.findValidByUserAndTenant(userId, tenantId)
                .chain(userRoles -> {
                    if (userRoles.isEmpty()) {
                        return Uni.createFrom().item(new HashSet<String>());
                    }

                    List<UUID> roleIds = userRoles.stream()
                            .map(UserRole::getRoleId)
                            .toList();

                    return roleRepository.findByIdsWithPermissions(tenantId, roleIds)
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

    public Uni<Boolean> hasRole(String roleName) {
        try {
            List<String> jwtRoles = jwt.getClaim("roles");
            if (jwtRoles != null && jwtRoles.contains(roleName)) {
                return Uni.createFrom().item(true);
            }

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

    public UUID getCurrentUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    public String getCurrentUserEmail() {
        return jwt.getClaim("email");
    }

    public String getCurrentUserName() {
        return jwt.getClaim("name");
    }

    public List<String> getCurrentUserRoles() {
        List<String> roles = jwt.getClaim("roles");
        return roles != null ? roles : new ArrayList<>();
    }

    public String getCurrentUserDepartment() {
        return jwt.getClaim("department");
    }

    public Uni<Boolean> isAdmin() {
        return hasRole("ADMIN");
    }

    public Uni<Boolean> isDoctor() {
        return hasRole("DOCTOR");
    }

    public Uni<Boolean> isNurse() {
        return hasRole("NURSE");
    }

    public Uni<Boolean> isPatient() {
        return hasRole("PATIENT");
    }

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
