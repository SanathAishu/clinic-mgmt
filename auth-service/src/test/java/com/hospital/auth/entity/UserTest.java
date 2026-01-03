package com.hospital.auth.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for User entity business logic.
 */
@DisplayName("User Entity Tests")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test-tenant", "John Doe", "john@example.com", "hashedPassword");
        user.setActive(true);
    }

    @Nested
    @DisplayName("Account Locking")
    class AccountLockingTests {

        @Test
        @DisplayName("isLocked returns false when lockedUntil is null")
        void isLocked_WhenLockedUntilIsNull_ReturnsFalse() {
            user.setLockedUntil(null);
            assertThat(user.isLocked()).isFalse();
        }

        @Test
        @DisplayName("isLocked returns true when lockedUntil is in the future")
        void isLocked_WhenLockedUntilInFuture_ReturnsTrue() {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            assertThat(user.isLocked()).isTrue();
        }

        @Test
        @DisplayName("isLocked returns false when lockedUntil is in the past")
        void isLocked_WhenLockedUntilInPast_ReturnsFalse() {
            user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
            assertThat(user.isLocked()).isFalse();
        }

        @Test
        @DisplayName("incrementFailedLoginAttempts locks account after threshold")
        void incrementFailedLoginAttempts_AfterThreshold_LocksAccount() {
            int lockoutThreshold = 5;
            int lockoutDuration = 30;

            // Increment 4 times - should not lock
            for (int i = 0; i < 4; i++) {
                user.incrementFailedLoginAttempts(lockoutThreshold, lockoutDuration);
            }
            assertThat(user.isLocked()).isFalse();
            assertThat(user.getFailedLoginAttempts()).isEqualTo(4);

            // 5th attempt - should lock
            user.incrementFailedLoginAttempts(lockoutThreshold, lockoutDuration);
            assertThat(user.isLocked()).isTrue();
            assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
            assertThat(user.getLockedUntil()).isNotNull();
            assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("resetFailedLoginAttempts clears attempts and unlocks")
        void resetFailedLoginAttempts_ClearsAttemptsAndUnlocks() {
            user.setFailedLoginAttempts(5);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));

            user.resetFailedLoginAttempts();

            assertThat(user.getFailedLoginAttempts()).isZero();
            assertThat(user.getLockedUntil()).isNull();
            assertThat(user.getLastLoginAt()).isNotNull();
            assertThat(user.isLocked()).isFalse();
        }
    }

    @Nested
    @DisplayName("Can Login")
    class CanLoginTests {

        @Test
        @DisplayName("canLogin returns true when active and not locked")
        void canLogin_WhenActiveAndNotLocked_ReturnsTrue() {
            user.setActive(true);
            user.setLockedUntil(null);
            assertThat(user.canLogin()).isTrue();
        }

        @Test
        @DisplayName("canLogin returns false when inactive")
        void canLogin_WhenInactive_ReturnsFalse() {
            user.setActive(false);
            assertThat(user.canLogin()).isFalse();
        }

        @Test
        @DisplayName("canLogin returns false when locked")
        void canLogin_WhenLocked_ReturnsFalse() {
            user.setActive(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            assertThat(user.canLogin()).isFalse();
        }
    }

    @Nested
    @DisplayName("Password Management")
    class PasswordManagementTests {

        @Test
        @DisplayName("updatePassword updates hash and timestamp")
        void updatePassword_UpdatesHashAndTimestamp() {
            LocalDateTime before = LocalDateTime.now();

            user.updatePassword("newHashedPassword");

            assertThat(user.getPasswordHash()).isEqualTo("newHashedPassword");
            assertThat(user.getPasswordChangedAt()).isNotNull();
            assertThat(user.getPasswordChangedAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor sets required fields correctly")
        void constructor_SetsRequiredFields() {
            User newUser = new User("tenant-1", "Jane Doe", "jane@example.com", "hash123");

            assertThat(newUser.getTenantId()).isEqualTo("tenant-1");
            assertThat(newUser.getName()).isEqualTo("Jane Doe");
            assertThat(newUser.getEmail()).isEqualTo("jane@example.com");
            assertThat(newUser.getPasswordHash()).isEqualTo("hash123");
            assertThat(newUser.getPasswordChangedAt()).isNotNull();
        }
    }
}
