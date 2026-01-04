package com.hospital.auth.service;

import com.hospital.auth.dto.AuthResponse;
import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterRequest;
import com.hospital.auth.dto.UserDto;
import com.hospital.auth.entity.User;
import com.hospital.auth.repository.UserRepository;
import com.hospital.auth.util.JwtUtils;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Authentication service with registration and login
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * Register a new user
     */
    @Transactional
    public Mono<AuthResponse> register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        return userRepository.existsByEmail(request.getEmail())
                .flatMap(emailExists -> {
                    if (Boolean.TRUE.equals(emailExists)) {
                        return Mono.error(new ValidationException("email", "Email already registered"));
                    }

                    // Create user entity with generated UUID
                    User user = User.builder()
                            .id(UUID.randomUUID())
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .name(request.getName())
                            .phone(request.getPhone())
                            .gender(request.getGender())
                            .role(request.getRole())
                            .active(true)
                            .build();

                    return userRepository.save(user);
                })
                .map(savedUser -> {
                    log.info("User registered successfully with ID: {}", savedUser.getId());

                    // Generate JWT token
                    String token = jwtUtils.generateToken(savedUser);
                    Long expiresIn = jwtUtils.getJwtExpiration() / 1000; // Convert to seconds

                    return AuthResponse.of(
                            savedUser.getId(),
                            savedUser.getEmail(),
                            savedUser.getName(),
                            savedUser.getRole(),
                            token,
                            expiresIn
                    );
                });
    }

    /**
     * Authenticate user and generate JWT token
     */
    @Transactional(readOnly = true)
    public Mono<AuthResponse> login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new NotFoundException("User", request.getEmail())))
                .flatMap(user -> {
                    // Check if user is active
                    if (!user.getActive()) {
                        return Mono.error(new ValidationException("Account is deactivated"));
                    }

                    // Verify password
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new ValidationException("Invalid email or password"));
                    }

                    log.info("User authenticated successfully: {}", user.getEmail());

                    // Generate JWT token
                    String token = jwtUtils.generateToken(user);
                    Long expiresIn = jwtUtils.getJwtExpiration() / 1000; // Convert to seconds

                    return Mono.just(AuthResponse.of(
                            user.getId(),
                            user.getEmail(),
                            user.getName(),
                            user.getRole(),
                            token,
                            expiresIn
                    ));
                });
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public Mono<UserDto> getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("User", userId)));
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public Mono<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("User", email)));
    }

    /**
     * Map User entity to DTO
     */
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
