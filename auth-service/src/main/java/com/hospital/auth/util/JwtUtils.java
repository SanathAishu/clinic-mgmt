package com.hospital.auth.util;

import com.hospital.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility class for RS256 token generation and validation
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.private-key-path}")
    private Resource privateKeyResource;

    @Value("${jwt.public-key-path}")
    private Resource publicKeyResource;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * Generate JWT token for authenticated user
     */
    public String generateToken(User user) {
        try {
            if (privateKey == null) {
                loadPrivateKey();
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("email", user.getEmail());
            claims.put("name", user.getName());
            claims.put("role", user.getRole().name());

            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpiration);

            return Jwts.builder()
                    .subject(user.getId().toString())
                    .claims(claims)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

        } catch (Exception e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            if (publicKey == null) {
                loadPublicKey();
            }

            Jwts.parser()
                    .verifyWith(publicKey)
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
            if (publicKey == null) {
                loadPublicKey();
            }

            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting user ID from token", e);
            return null;
        }
    }

    /**
     * Get public key as PEM string for API Gateway
     */
    public String getPublicKeyPem() {
        try {
            if (publicKey == null) {
                loadPublicKey();
            }

            byte[] encoded = publicKey.getEncoded();
            String base64 = Base64.getEncoder().encodeToString(encoded);

            return "-----BEGIN PUBLIC KEY-----\n" +
                    base64.replaceAll("(.{64})", "$1\n") +
                    "\n-----END PUBLIC KEY-----";

        } catch (Exception e) {
            log.error("Error getting public key PEM", e);
            throw new RuntimeException("Failed to get public key", e);
        }
    }

    /**
     * Load private key from PEM file
     */
    private void loadPrivateKey() throws Exception {
        try {
            String keyContent = Files.readString(privateKeyResource.getFile().toPath())
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(spec);

            log.info("Private key loaded successfully");
        } catch (IOException e) {
            log.error("Failed to load private key", e);
            throw new RuntimeException("Failed to load private key", e);
        }
    }

    /**
     * Load public key from PEM file
     */
    private void loadPublicKey() throws Exception {
        try {
            String keyContent = Files.readString(publicKeyResource.getFile().toPath())
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(spec);

            log.info("Public key loaded successfully");
        } catch (IOException e) {
            log.error("Failed to load public key", e);
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    public Long getJwtExpiration() {
        return jwtExpiration;
    }
}
