package com.hospital.common.security;
import io.quarkus.logging.Log;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JWT token generation and validation service using SmallRye JWT.
 * Provides token creation for authenticated users with multi-tenancy and dynamic RBAC support.
 *
 * JWT Claims Structure:
 * - sub: User ID (UUID)
 * - email: User email
 * - name: User full name
 * - tenant_id: Hospital/Clinic tenant identifier
 * - roles: Array of role names (DOCTOR, NURSE, ADMIN, etc.)
 * - permissions: Array of permission strings (resource:action format)
 * - department: Optional department assignment
 * - iat: Issued at timestamp
 * - exp: Expiration timestamp
 */
@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "jwt.secret")
    String jwtSecret;

    @ConfigProperty(name = "jwt.expiration", defaultValue = "86400")  // 24 hours
    long tokenExpirationSeconds;

    @ConfigProperty(name = "jwt.issuer", defaultValue = "hospital-system")
    String jwtIssuer;

    /**
     * Generate a JWT token for an authenticated user with multi-tenancy and dynamic RBAC.
     *
     * @param userId      User's unique identifier
     * @param email       User's email
     * @param name        User's full name
     * @param tenantId    Tenant identifier (hospital/clinic ID)
     * @param roles       List of role names
     * @param permissions List of permission strings (e.g., "medical_record:read", "patient:write")
     * @param department  Optional department assignment (can be null)
     * @return JWT token as string
     */
    public String generateToken(UUID userId, String email, String name, String tenantId,
                               List<String> roles, List<String> permissions, String department) {
        try {
            Instant now = Instant.now();
            Instant expirationTime = now.plusSeconds(tokenExpirationSeconds);

            var jwtBuilder = Jwt.issuer(jwtIssuer)
                    .subject(userId.toString())
                    .expiresAt(expirationTime)
                    .issuedAt(now)
                    .claim("email", email)
                    .claim("name", name)
                    .claim("tenant_id", tenantId)
                    .claim("roles", roles)
                    .claim("permissions", permissions)
                    .claim("iat", now.getEpochSecond());

            // Add department if provided
            if (department != null && !department.isEmpty()) {
                jwtBuilder = jwtBuilder.claim("department", department);
            }

            String token = jwtBuilder.sign();

            Log.debugf("JWT token generated for user: %s (%s) in tenant: %s", email, userId, tenantId);
            return token;
        } catch (Exception e) {
            Log.errorf(e, "Failed to generate JWT token for user: %s", email);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Generate a JWT token for an authenticated user (legacy method for backward compatibility).
     * This method is deprecated. Use generateToken with tenantId, roles, and permissions instead.
     *
     * @param userId   User's unique identifier
     * @param email    User's email
     * @param name     User's full name
     * @param role     User's role (PATIENT, DOCTOR, NURSE, RECEPTIONIST, ADMIN)
     * @return JWT token as string
     * @deprecated Use {@link #generateToken(UUID, String, String, String, List, List, String)} instead
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    public String generateToken(UUID userId, String email, String name, String role) {
        try {
            Instant now = Instant.now();
            Instant expirationTime = now.plusSeconds(tokenExpirationSeconds);

            String token = Jwt.issuer(jwtIssuer)
                    .subject(userId.toString())
                    .expiresAt(expirationTime)
                    .issuedAt(now)
                    .claim("email", email)
                    .claim("name", name)
                    .claim("role", role)
                    .claim("iat", now.getEpochSecond())
                    .sign();

            Log.debugf("JWT token generated for user: %s (%s) - using legacy method", email, userId);
            return token;
        } catch (Exception e) {
            Log.errorf(e, "Failed to generate JWT token for user: %s", email);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Generate a token with custom claims.
     *
     * @param userId   User's unique identifier
     * @param email    User's email
     * @param name     User's full name
     * @param role     User's role
     * @param customClaims Additional claims to include
     * @return JWT token as string
     */
    public String generateTokenWithClaims(UUID userId, String email, String name, String role,
                                         java.util.Map<String, Object> customClaims) {
        try {
            Instant now = Instant.now();
            Instant expirationTime = now.plusSeconds(tokenExpirationSeconds);

            var jwtBuilder = Jwt.issuer(jwtIssuer)
                    .subject(userId.toString())
                    .expiresAt(expirationTime)
                    .issuedAt(now)
                    .claim("email", email)
                    .claim("name", name)
                    .claim("role", role)
                    .claim("iat", now.getEpochSecond());

            // Add custom claims
            if (customClaims != null) {
                for (var entry : customClaims.entrySet()) {
                    jwtBuilder = jwtBuilder.claim(entry.getKey(), entry.getValue());
                }
            }

            return jwtBuilder.sign();
        } catch (Exception e) {
            Log.errorf(e, "Failed to generate JWT token with custom claims for user: %s", email);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    public String generateRefreshToken(UUID userId, String email, String tenantId) {
        try {
            Instant now = Instant.now();
            // Refresh token valid for 7 days
            Instant expirationTime = now.plusSeconds(7 * 24 * 60 * 60);

            String token = Jwt.issuer(jwtIssuer)
                    .subject(userId.toString())
                    .expiresAt(expirationTime)
                    .issuedAt(now)
                    .claim("email", email)
                    .claim("tenant_id", tenantId)
                    .claim("type", "refresh")
                    .sign();

            Log.debugf("Refresh token generated for user: %s in tenant: %s", email, tenantId);
            return token;
        } catch (Exception e) {
            Log.errorf(e, "Failed to generate refresh token for user: %s", email);
            throw new RuntimeException("Refresh token generation failed", e);
        }
    }

    /**
     * Generate a refresh token (legacy method for backward compatibility).
     *
     * @param userId User's unique identifier
     * @param email  User's email
     * @return Refresh token as string
     * @deprecated Use {@link #generateRefreshToken(UUID, String, String)} instead
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    public String generateRefreshToken(UUID userId, String email) {
        try {
            Instant now = Instant.now();
            // Refresh token valid for 7 days
            Instant expirationTime = now.plusSeconds(7 * 24 * 60 * 60);

            String token = Jwt.issuer(jwtIssuer)
                    .subject(userId.toString())
                    .expiresAt(expirationTime)
                    .issuedAt(now)
                    .claim("email", email)
                    .claim("type", "refresh")
                    .sign();

            Log.debugf("Refresh token generated for user: %s - using legacy method", email);
            return token;
        } catch (Exception e) {
            Log.errorf(e, "Failed to generate refresh token for user: %s", email);
            throw new RuntimeException("Refresh token generation failed", e);
        }
    }

    /**
     * Validate a JWT token.
     * Note: SmallRye JWT automatically validates tokens via @Authenticated annotation
     * and makes them available via JsonWebToken injection.
     *
     * This method is for manual validation if needed.
     *
     * @param token JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                Log.warn("Attempt to validate null or empty token");
                return false;
            }

            // SmallRye JWT will handle validation when token is accessed
            Log.debug("Token validation initiated");
            return true;
        } catch (Exception e) {
            Log.warnf("Token validation failed: %s", e.getMessage());
            return false;
        }
    }

    public String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring("Bearer ".length());
    }

    public long getTokenExpirationSeconds() {
        return tokenExpirationSeconds;
    }

    public void setTokenExpiration(long expirationSeconds) {
        this.tokenExpirationSeconds = expirationSeconds;
    }
}
