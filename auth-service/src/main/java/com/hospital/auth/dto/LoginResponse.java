package com.hospital.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Login response DTO containing JWT token and user information.
 *
 * JWT Claims Include:
 * - sub: userId
 * - tenantId: tenant discriminator
 * - email: user's email
 * - roles: array of role names
 * - permissions: array of permission names
 * - iss: issuer (hospital-system)
 * - exp: expiration timestamp
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private long expiresIn; // Seconds until expiration
    private UserDto user;

    public LoginResponse() {
    }

    public LoginResponse(String token, long expiresIn, UserDto user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}
