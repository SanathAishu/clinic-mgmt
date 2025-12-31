package com.hospital.auth.dto;

import com.hospital.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Authentication response with JWT token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String email;
    private String name;
    private Role role;
    private String accessToken;
    private String tokenType;
    private Long expiresIn;  // seconds

    public static AuthResponse of(UUID userId, String email, String name, Role role, String token, Long expiresIn) {
        return AuthResponse.builder()
                .userId(userId)
                .email(email)
                .name(name)
                .role(role)
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
