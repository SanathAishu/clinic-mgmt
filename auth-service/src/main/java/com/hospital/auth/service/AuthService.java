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
 * User authentication with JWT generation, multi-tenancy, and RBAC.
 * Security: tenantId from trusted source only (header/subdomain), not request body.
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

    public Uni<LoginResponse> login(String tenantId, LoginRequest request) {
        return userRepository.findByEmail(tenantId, request.getEmail())
            .onItem().ifNull().failWith(() -> new UnauthorizedException("Invalid email or password"))
            .chain(user -> {
                if (user.isLocked()) {
                    return Uni.createFrom().failure(
                        new ForbiddenException("Account is temporarily locked. Please try again later.")
                    );
                }

                if (!user.isActive()) {
                    return Uni.createFrom().failure(
                        new ForbiddenException("Account is inactive. Please contact support.")
                    );
                }

                if (!BcryptUtil.matches(request.getPassword(), user.getPasswordHash())) {
                    user.incrementFailedLoginAttempts(lockoutThreshold, lockoutDurationMinutes);
                    return userRepository.persist(user)
                        .chain(() -> Uni.createFrom().failure(
                            new UnauthorizedException("Invalid email or password")
                        ));
                }

                user.resetFailedLoginAttempts();

                return userRepository.persist(user)
                    .chain(() -> getUserRolesAndPermissions(tenantId, user.getId()))
                    .chain(rolesAndPerms -> {
                        String token = generateJwtToken(user, rolesAndPerms.roles, rolesAndPerms.permissions);
                        UserDto userDto = toUserDto(user);
                        userDto.setRoles(rolesAndPerms.roles);
                        userDto.setPermissions(rolesAndPerms.permissions);

                        return Uni.createFrom().item(
                            new LoginResponse(token, jwtExpirationSeconds, userDto)
                        );
                    });
            });
    }

    @WithSession
    public Uni<UserDto> register(String tenantId, RegisterRequest request) {
        return userRepository.existsByEmail(tenantId, request.getEmail())
            .chain(exists -> {
                if (exists) {
                    return Uni.createFrom().failure(
                        new ForbiddenException("Email already registered")
                    );
                }

                String passwordHash = BcryptUtil.bcryptHash(request.getPassword());
                User user = new User(
                    tenantId,
                    request.getName(),
                    request.getEmail(),
                    passwordHash
                );
                user.setPhone(request.getPhone());
                user.setActive(true);
                user.setEmailVerified(false);

                return userRepository.persist(user)
                    .chain(savedUser -> {
                        UserRegisteredEvent event = new UserRegisteredEvent(
                            tenantId,
                            savedUser.getId(),
                            savedUser.getName(),
                            savedUser.getEmail(),
                            savedUser.getPhone()
                        );

                        Log.infof("User registered: %s for tenant: %s", savedUser.getEmail(), tenantId);

                        return Uni.createFrom().completionStage(userEventEmitter.send(event))
                            .map(ignore -> toUserDto(savedUser))
                            .onFailure().invoke(error ->
                                Log.errorf(error, "Failed to publish UserRegisteredEvent for user: %s",
                                    savedUser.getId())
                            );
                    });
            });
    }

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

    private Uni<RolesAndPermissions> getUserRolesAndPermissions(String tenantId, java.util.UUID userId) {
        return userRoleRepository.findValidByUserAndTenant(userId, tenantId)
            .chain(userRoles -> {
                if (userRoles.isEmpty()) {
                    return Uni.createFrom().item(
                        new RolesAndPermissions(List.of(), Set.of())
                    );
                }

                List<java.util.UUID> roleIds = userRoles.stream()
                    .map(ur -> ur.getRoleId())
                    .toList();

                return roleRepository.findByIdsWithPermissions(tenantId, roleIds)
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

    private static class RolesAndPermissions {
        final List<String> roles;
        final Set<String> permissions;

        RolesAndPermissions(List<String> roles, Set<String> permissions) {
            this.roles = roles;
            this.permissions = permissions;
        }
    }
}
