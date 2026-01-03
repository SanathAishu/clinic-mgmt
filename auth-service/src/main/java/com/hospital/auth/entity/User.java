package com.hospital.auth.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity with multi-tenancy support.
 *
 * Multi-Tenancy:
 * - tenantId field ensures data isolation
 * - email is unique per tenant (not globally)
 * - All queries MUST filter by tenantId
 *
 * RBAC Integration:
 * - Users have roles assigned via user_roles table
 * - Permissions derived from role_permissions
 * - See PermissionService for runtime authorization
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "email"}) // Email unique per tenant
}, indexes = {
    @Index(name = "idx_user_tenant_email", columnList = "tenant_id, email"),
    @Index(name = "idx_user_tenant_active", columnList = "tenant_id, active"),
    @Index(name = "idx_user_created_at", columnList = "created_at")
})
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant discriminator - CRITICAL for data isolation.
     * Must be set on creation and NEVER modified.
     */
    @Column(name = "tenant_id", nullable = false, length = 50, updatable = false)
    private String tenantId;

    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Email is unique per tenant (not globally).
     * Used for login along with tenantId.
     */
    @Column(nullable = false, length = 255)
    private String email;

    /**
     * Password hash (BCrypt).
     * NEVER expose in DTOs or APIs.
     */
    @Column(nullable = false, length = 255)
    private String passwordHash;

    /**
     * Phone number (optional, for 2FA or notifications).
     */
    @Column(length = 20)
    private String phone;

    /**
     * Active status - inactive users cannot login.
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Email verification status.
     */
    @Column(nullable = false)
    private boolean emailVerified = false;

    /**
     * Failed login attempts counter (for account lockout).
     */
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    /**
     * Account locked until timestamp (null if not locked).
     */
    @Column
    private LocalDateTime lockedUntil;

    /**
     * Last successful login timestamp.
     */
    @Column
    private LocalDateTime lastLoginAt;

    /**
     * Last password change timestamp.
     */
    @Column
    private LocalDateTime passwordChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors

    public User() {
    }

    public User(String tenantId, String name, String email, String passwordHash) {
        this.tenantId = tenantId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = LocalDateTime.now();
    }

    // Business Logic Methods

    /**
     * Check if account is locked.
     */
    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    /**
     * Check if user can login.
     */
    public boolean canLogin() {
        return active && !isLocked();
    }

    /**
     * Increment failed login attempts and lock account if threshold exceeded.
     *
     * @param lockoutThreshold Number of attempts before lockout
     * @param lockoutDurationMinutes How long to lock account
     */
    public void incrementFailedLoginAttempts(int lockoutThreshold, int lockoutDurationMinutes) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= lockoutThreshold) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
        }
    }

    /**
     * Reset failed login attempts after successful login.
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Update password hash.
     */
    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
