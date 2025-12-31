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
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("email", "Email already registered");
        }

        // Create user entity
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .gender(request.getGender())
                .role(request.getRole())
                .active(true)
                .build();

        // Save user
        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}", user.getId());

        // Generate JWT token
        String token = jwtUtils.generateToken(user);
        Long expiresIn = jwtUtils.getJwtExpiration() / 1000; // Convert to seconds

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                token,
                expiresIn
        );
    }

    /**
     * Authenticate user and generate JWT token
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User", request.getEmail()));

        // Check if user is active
        if (!user.getActive()) {
            throw new ValidationException("Account is deactivated");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ValidationException("Invalid email or password");
        }

        log.info("User authenticated successfully: {}", user.getEmail());

        // Generate JWT token
        String token = jwtUtils.generateToken(user);
        Long expiresIn = jwtUtils.getJwtExpiration() / 1000; // Convert to seconds

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                token,
                expiresIn
        );
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        return mapToDto(user);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        return mapToDto(user);
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
