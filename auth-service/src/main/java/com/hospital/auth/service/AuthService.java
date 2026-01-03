package com.hospital.auth.service;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.LoginResponse;
import com.hospital.auth.dto.RegisterRequest;
import com.hospital.auth.dto.UserDto;
import com.hospital.auth.entity.User;
import com.hospital.auth.event.UserRegisteredEvent;
import com.hospital.auth.repository.UserRepository;
import com.hospital.common.exception.ForbiddenException;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.UnauthorizedException;
import com.hospital.common.rbac.repository.RoleRepository;
import com.hospital.common.rbac.repository.UserRoleRepository;
import com.hospital.common.rbac.service.PermissionService;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.logging.Log;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authentication service with multi-tenancy and RBAC integration.
 *
 * Responsibilities:
 * - User login with JWT generation
 * - User registration
 * - Password validation
 * - Account lockout management
 * - JWT token generation with roles and permissions
 */
@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    PermissionService permissionService;

    @Inject
    UserRoleRepository userRoleRepository;

    @Inject
    RoleRepository roleRepository;

    @Channel("user-events")
    Emitter<UserRegisteredEvent> userEventEmitter;

    @ConfigProperty(name = "jwt.issuer", defaultValue = "hospital-system")
    String jwtIssuer;

    @ConfigProperty(name = "jwt.expiration-seconds", defaultValue = "86400")
    long jwtExpirationSeconds;

    @ConfigProperty(name = "auth.lockout-threshold", defaultValue = "5")
    int lockoutThreshold;

    @ConfigProperty(name = "auth.lockout-duration-minutes", defaultValue = "30")
    int lockoutDurationMinutes;

    /**
     * Authenticate user and generate JWT token.
     *
     * Security:
     * - tenantId MUST come from trusted source (subdomain/header), NOT request body
     * - Password verified using BCrypt
     * - Account lockout after failed attempts
     * - JWT contains tenantId, userId, roles, permissions
     *
     * @param tenantId Tenant ID (from subdomain/header)
     * @param request Login credentials
     * @return Login response with JWT and user info
     */
    public Uni<LoginResponse> login(String tenantId, LoginRequest request) {
        return userRepository.findByEmail(tenantId, request.getEmail())
            .onItem().ifNull().failWith(() -> new UnauthorizedException("Invalid email or password"))
            .chain(user -> {
                // Check if account is locked
                if (user.isLocked()) {
                    return Uni.createFrom().failure(
                        new ForbiddenException("Account is temporarily locked. Please try again later.")
                    );
                }

                // Check if account is active
                if (!user.isActive()) {
                    return Uni.createFrom().failure(
                        new ForbiddenException("Account is inactive. Please contact support.")
                    );
                }

                // Verify password
                if (!BcryptUtil.matches(request.getPassword(), user.getPasswordHash())) {
                    // Increment failed login attempts
                    user.incrementFailedLoginAttempts(lockoutThreshold, lockoutDurationMinutes);
                    return userRepository.persist(user)
                        .chain(() -> Uni.createFrom().failure(
                            new UnauthorizedException("Invalid email or password")
                        ));
                }

                // Password correct - reset failed attempts
                user.resetFailedLoginAttempts();

                // Get user's roles and permissions
                return userRepository.persist(user)
                    .chain(() -> getUserRolesAndPermissions(tenantId, user.getId()))
                    .chain(rolesAndPerms -> {
                        List<String> roles = rolesAndPerms.roles;
                        Set<String> permissions = rolesAndPerms.permissions;

                        // Generate JWT token
                        String token = generateJwtToken(user, roles, permissions);

                        // Build user DTO
                        UserDto userDto = toUserDto(user);
                        userDto.setRoles(roles);
                        userDto.setPermissions(permissions);

                        return Uni.createFrom().item(
                            new LoginResponse(token, jwtExpirationSeconds, userDto)
                        );
                    });
            });
    }

    /**
     * Register new user.
     *
     * Security:
     * - tenantId from trusted source
     * - Email uniqueness checked per tenant
     * - Password hashed with BCrypt
     * - Default role assigned (configurable)
     *
     * @param tenantId Tenant ID
     * @param request Registration data
     * @return Created user DTO
     */
    @WithSession
    public Uni<UserDto> register(String tenantId, RegisterRequest request) {
        // Check if email already exists
        return userRepository.existsByEmail(tenantId, request.getEmail())
            .chain(exists -> {
                if (exists) {
                    return Uni.createFrom().failure(
                        new ForbiddenException("Email already registered")
                    );
                }

                // Hash password
                String passwordHash = BcryptUtil.bcryptHash(request.getPassword());

                // Create user entity
                User user = new User(
                    tenantId,
                    request.getName(),
                    request.getEmail(),
                    passwordHash
                );
                user.setPhone(request.getPhone());
                user.setActive(true);
                user.setEmailVerified(false);

                // Persist user
                return userRepository.persist(user)
                    .chain(savedUser -> {
                        // TODO: Assign default role (e.g., PATIENT)

                        // Publish UserRegisteredEvent to RabbitMQ
                        UserRegisteredEvent event = new UserRegisteredEvent(
                            tenantId,
                            savedUser.getId(),
                            savedUser.getName(),
                            savedUser.getEmail(),
                            savedUser.getPhone()
                        );

                        Log.infof("User registered: %s for tenant: %s, publishing event...",
                            savedUser.getEmail(), tenantId);

                        return Uni.createFrom().completionStage(userEventEmitter.send(event))
                            .map(ignore -> toUserDto(savedUser))
                            .onFailure().invoke(error ->
                                Log.errorf(error, "Failed to publish UserRegisteredEvent for user: %s",
                                    savedUser.getId())
                            );
                    });
            });
    }

    /**
     * Generate JWT token with tenant and RBAC claims.
     *
     * Claims:
     * - sub: userId (UUID)
     * - tenantId: tenant discriminator
     * - email: user's email
     * - name: user's name
     * - roles: array of role names
     * - permissions: array of permission names
     * - iss: issuer
     * - iat: issued at
     * - exp: expiration
     *
     * @param user User entity
     * @param roles Role names
     * @param permissions Permission names
     * @return JWT token string
     */
    private String generateJwtToken(User user, List<String> roles, Set<String> permissions) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtExpirationSeconds);

        return Jwt.issuer(jwtIssuer)
            .subject(user.getId().toString())
            .claim("tenantId", user.getTenantId())
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .claim("roles", roles)
            .claim("permissions", permissions)
            .issuedAt(now)
            .expiresAt(expiry)
            .sign();
    }

    /**
     * Get user's roles and permissions from RBAC tables.
     *
     * @param tenantId Tenant ID
     * @param userId User ID
     * @return Roles and permissions wrapper
     */
    private Uni<RolesAndPermissions> getUserRolesAndPermissions(String tenantId, java.util.UUID userId) {
        // Get user's active roles
        return userRoleRepository.findValidByUserAndTenant(userId, tenantId)
            .chain(userRoles -> {
                if (userRoles.isEmpty()) {
                    // No roles assigned - return empty
                    return Uni.createFrom().item(
                        new RolesAndPermissions(List.of(), Set.of())
                    );
                }

                // Get role UUIDs
                List<java.util.UUID> roleIds = userRoles.stream()
                    .map(ur -> ur.getRoleId())
                    .toList();

                // Get role details with permissions
                return roleRepository.findByIdsAndTenant(tenantId, roleIds)
                    .map(roles -> {
                        List<String> roleNames = roles.stream()
                            .filter(r -> r.isActive())
                            .map(r -> r.getName())
                            .toList();

                        Set<String> permissionNames = new HashSet<>();
                        roles.stream()
                            .filter(r -> r.isActive())
                            .forEach(role -> {
                                role.getPermissions().forEach(perm -> {
                                    permissionNames.add(perm.getName());
                                });
                            });

                        return new RolesAndPermissions(roleNames, permissionNames);
                    });
            });
    }

    /**
     * Convert User entity to UserDto.
     */
    private UserDto toUserDto(User user) {
        return new UserDto(
            user.getId(),
            user.getTenantId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.isActive(),
            user.isEmailVerified(),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    /**
     * Helper class to hold roles and permissions.
     */
    private static class RolesAndPermissions {
        final List<String> roles;
        final Set<String> permissions;

        RolesAndPermissions(List<String> roles, Set<String> permissions) {
            this.roles = roles;
            this.permissions = permissions;
        }
    }
}
