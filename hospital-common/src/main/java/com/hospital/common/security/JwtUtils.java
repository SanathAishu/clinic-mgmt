package com.hospital.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Shared JWT utility class for HS512 token validation
 * Used by all services that need to validate JWT tokens
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret:HospitalManagementSystemSecretKeyForHS512Algorithm2024SecureTokenGeneration}")
    private String jwtSecret;

    private SecretKey secretKey;

    /**
     * Get or create the secret key
     */
    private SecretKey getSecretKey() {
        if (secretKey == null) {
            // Ensure the secret is at least 64 bytes for HS512
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 64) {
                // Pad the key to 64 bytes if needed
                byte[] paddedKey = new byte[64];
                System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
                secretKey = Keys.hmacShaKeyFor(paddedKey);
            } else {
                secretKey = Keys.hmacShaKeyFor(keyBytes);
            }
        }
        return secretKey;
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user ID from JWT token
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null ? claims.getSubject() : null;
        } catch (Exception e) {
            log.error("Error extracting user ID from token", e);
            return null;
        }
    }

    /**
     * Extract claims from JWT token
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error extracting claims from token", e);
            return null;
        }
    }

    /**
     * Extract email from JWT token
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("email", String.class) : null;
    }

    /**
     * Extract role from JWT token
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * Extract name from JWT token
     */
    public String getNameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("name", String.class) : null;
    }
}
