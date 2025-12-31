package com.hospital.auth.controller;

import com.hospital.auth.dto.AuthResponse;
import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterRequest;
import com.hospital.auth.dto.UserDto;
import com.hospital.auth.service.AuthService;
import com.hospital.auth.util.JwtUtils;
import com.hospital.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authentication controller with registration, login, and public key endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    /**
     * Register a new user (patient or doctor)
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Get public key for JWT validation (used by API Gateway)
     */
    @GetMapping("/public-key")
    public ResponseEntity<String> getPublicKey() {
        log.info("Public key requested");

        String publicKeyPem = jwtUtils.getPublicKeyPem();

        return ResponseEntity.ok(publicKeyPem);
    }

    /**
     * Get user by ID (authenticated endpoint)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID userId) {
        log.info("Fetching user with ID: {}", userId);

        UserDto user = authService.getUserById(userId);

        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Get user by email (authenticated endpoint)
     */
    @GetMapping("/users/email/{email}")
    public ResponseEntity<ApiResponse<UserDto>> getUserByEmail(@PathVariable String email) {
        log.info("Fetching user with email: {}", email);

        UserDto user = authService.getUserByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running");
    }
}
