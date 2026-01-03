package com.hospital.auth.service;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.LoginResponse;
import com.hospital.auth.dto.RegisterRequest;
import com.hospital.auth.dto.UserDto;
import com.hospital.auth.entity.User;
import com.hospital.auth.event.UserRegisteredEvent;
import com.hospital.auth.repository.UserRepository;
import com.hospital.common.exception.ForbiddenException;
import com.hospital.common.exception.UnauthorizedException;
import com.hospital.common.rbac.entity.Role;
import com.hospital.common.rbac.entity.UserRole;
import com.hospital.common.rbac.repository.RoleRepository;
import com.hospital.common.rbac.repository.UserRoleRepository;
import com.hospital.common.rbac.service.PermissionService;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 */
@QuarkusTest
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Inject
    AuthService authService;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    UserRoleRepository userRoleRepository;

    @InjectMock
    RoleRepository roleRepository;

    @InjectMock
    PermissionService permissionService;

    private static final String TENANT_ID = "test-tenant";
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_PASSWORD = "SecurePass123!";
    private static final String TEST_NAME = "John Doe";

    private User createTestUser() {
        String hashedPassword = BcryptUtil.bcryptHash(TEST_PASSWORD);
        User user = new User(TENANT_ID, TEST_NAME, TEST_EMAIL, hashedPassword);
        user.setId(UUID.randomUUID());
        user.setActive(true);
        user.setEmailVerified(true);
        return user;
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("login succeeds with valid credentials")
        void login_WithValidCredentials_Succeeds() {
            // Arrange
            User user = createTestUser();
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            when(userRepository.findByEmail(TENANT_ID, TEST_EMAIL))
                .thenReturn(Uni.createFrom().item(user));
            when(userRepository.persist(any(User.class)))
                .thenReturn(Uni.createFrom().item(user));
            when(userRoleRepository.findValidByUserAndTenant(any(UUID.class), eq(TENANT_ID)))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

            // Act
            UniAssertSubscriber<LoginResponse> subscriber = authService
                .login(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            LoginResponse response = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isNotBlank();
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("login fails with wrong password")
        void login_WithWrongPassword_Fails() {
            // Arrange
            User user = createTestUser();
            LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongPassword");

            when(userRepository.findByEmail(TENANT_ID, TEST_EMAIL))
                .thenReturn(Uni.createFrom().item(user));
            when(userRepository.persist(any(User.class)))
                .thenReturn(Uni.createFrom().item(user));

            // Act
            UniAssertSubscriber<LoginResponse> subscriber = authService
                .login(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            subscriber.awaitFailure(Duration.ofSeconds(5))
                .assertFailedWith(UnauthorizedException.class);
        }

        @Test
        @DisplayName("login fails when user not found")
        void login_WhenUserNotFound_Fails() {
            // Arrange
            LoginRequest request = new LoginRequest("nonexistent@example.com", TEST_PASSWORD);

            when(userRepository.findByEmail(TENANT_ID, "nonexistent@example.com"))
                .thenReturn(Uni.createFrom().nullItem());

            // Act
            UniAssertSubscriber<LoginResponse> subscriber = authService
                .login(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            subscriber.awaitFailure(Duration.ofSeconds(5))
                .assertFailedWith(UnauthorizedException.class);
        }

        @Test
        @DisplayName("login fails when account is locked")
        void login_WhenAccountLocked_Fails() {
            // Arrange
            User user = createTestUser();
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            when(userRepository.findByEmail(TENANT_ID, TEST_EMAIL))
                .thenReturn(Uni.createFrom().item(user));

            // Act
            UniAssertSubscriber<LoginResponse> subscriber = authService
                .login(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            subscriber.awaitFailure(Duration.ofSeconds(5))
                .assertFailedWith(ForbiddenException.class);
        }

        @Test
        @DisplayName("login fails when account is inactive")
        void login_WhenAccountInactive_Fails() {
            // Arrange
            User user = createTestUser();
            user.setActive(false);
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            when(userRepository.findByEmail(TENANT_ID, TEST_EMAIL))
                .thenReturn(Uni.createFrom().item(user));

            // Act
            UniAssertSubscriber<LoginResponse> subscriber = authService
                .login(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            subscriber.awaitFailure(Duration.ofSeconds(5))
                .assertFailedWith(ForbiddenException.class);
        }

        @Test
        @DisplayName("login increments failed attempts on wrong password")
        void login_WithWrongPassword_IncrementsFailedAttempts() {
            // Arrange
            User user = createTestUser();
            user.setFailedLoginAttempts(0);
            LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongPassword");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.findByEmail(TENANT_ID, TEST_EMAIL))
                .thenReturn(Uni.createFrom().item(user));
            when(userRepository.persist(userCaptor.capture()))
                .thenReturn(Uni.createFrom().item(user));

            // Act
            authService.login(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(5));

            // Assert
            User persistedUser = userCaptor.getValue();
            assertThat(persistedUser.getFailedLoginAttempts()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("register succeeds with valid data")
        void register_WithValidData_Succeeds() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);
            request.setPhone("+1234567890");

            when(userRepository.existsByEmail(TENANT_ID, TEST_EMAIL))
                .thenReturn(Uni.createFrom().item(false));

            User savedUser = createTestUser();
            when(userRepository.persist(any(User.class)))
                .thenReturn(Uni.createFrom().item(savedUser));

            // Act
            UniAssertSubscriber<UserDto> subscriber = authService
                .register(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            UserDto result = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(result.getName()).isEqualTo(TEST_NAME);
        }

        @Test
        @DisplayName("register fails when email already exists")
        void register_WhenEmailExists_Fails() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);

            when(userRepository.existsByEmail(TENANT_ID, TEST_EMAIL))
                .thenReturn(Uni.createFrom().item(true));

            // Act
            UniAssertSubscriber<UserDto> subscriber = authService
                .register(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

            // Assert
            subscriber.awaitFailure(Duration.ofSeconds(5))
                .assertFailedWith(ForbiddenException.class);
        }

        @Test
        @DisplayName("register hashes password before saving")
        void register_HashesPassword() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);

            when(userRepository.existsByEmail(TENANT_ID, TEST_EMAIL))
                .thenReturn(Uni.createFrom().item(false));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            User savedUser = createTestUser();
            when(userRepository.persist(userCaptor.capture()))
                .thenReturn(Uni.createFrom().item(savedUser));

            // Act
            authService.register(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(5));

            // Assert
            User persistedUser = userCaptor.getValue();
            assertThat(persistedUser.getPasswordHash()).isNotEqualTo(TEST_PASSWORD);
            assertThat(BcryptUtil.matches(TEST_PASSWORD, persistedUser.getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("register sets correct tenant ID")
        void register_SetsCorrectTenantId() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);

            when(userRepository.existsByEmail(TENANT_ID, TEST_EMAIL))
                .thenReturn(Uni.createFrom().item(false));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            User savedUser = createTestUser();
            when(userRepository.persist(userCaptor.capture()))
                .thenReturn(Uni.createFrom().item(savedUser));

            // Act
            authService.register(TENANT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(5));

            // Assert
            User persistedUser = userCaptor.getValue();
            assertThat(persistedUser.getTenantId()).isEqualTo(TENANT_ID);
        }
    }
}
