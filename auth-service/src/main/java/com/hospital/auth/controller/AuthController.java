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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());

        return authService.register(request)
                .map(response -> ApiResponse.success("User registered successfully", response));
    }

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public Mono<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());

        return authService.login(request)
                .map(response -> ApiResponse.success("Login successful", response));
    }

    /**
     * Validate token endpoint (for internal service calls)
     */
    @GetMapping("/validate")
    public Mono<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ApiResponse.success(false));
        }
        String token = authHeader.substring(7);
        boolean isValid = jwtUtils.validateToken(token);
        return Mono.just(ApiResponse.success(isValid));
    }

    /**
     * Get user by ID (authenticated endpoint)
     */
    @GetMapping("/users/{userId}")
    public Mono<ApiResponse<UserDto>> getUserById(@PathVariable UUID userId) {
        log.info("Fetching user with ID: {}", userId);

        return authService.getUserById(userId)
                .map(ApiResponse::success);
    }

    /**
     * Get user by email (authenticated endpoint)
     */
    @GetMapping("/users/email/{email}")
    public Mono<ApiResponse<UserDto>> getUserByEmail(@PathVariable String email) {
        log.info("Fetching user with email: {}", email);

        return authService.getUserByEmail(email)
                .map(ApiResponse::success);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Auth Service is running");
    }
}
