package com.hospital.common.rbac.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User-Resource-Permission mapping for resource-level access control.
 *
 * This enables fine-grained permissions where a user can have specific
 * permissions on individual resources, beyond their role-based permissions.
 *
 * Use Cases:
 * 1. Doctor can read/write medical records for their assigned patients only
 * 2. Nurse can access specific patients in their ward
 * 3. Receptionist can manage appointments for specific doctors
 * 4. Break-glass access: Temporary emergency access to patient records
 *
 * Examples:
 * - User: Dr. Smith
 *   Resource: medical_record
 *   Resource ID: patient-123-record-456
 *   Permission: read, write
 *   Reason: Assigned doctor for this patient
 *
 * - User: Nurse Jane
 *   Resource: patient
 *   Resource ID: patient-789
 *   Permission: read
 *   Reason: Patient in her ward
 */
@Entity
@Table(name = "user_resource_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "resource_type", "resource_id", "permission", "tenant_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserResourcePermission extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(nullable = false, length = 50)
    private String permission;

    @Column(length = 255)
    private String reason;

    @Column(name = "is_break_glass")
    private boolean isBreakGlass = false;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @PrePersist
    protected void onCreate() {
        grantedAt = LocalDateTime.now();
        if (validFrom == null) {
            validFrom = LocalDateTime.now();
        }
    }

    public boolean isValid() {
        if (!active || revokedAt != null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }

        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }

        return true;
    }

    public void revoke(UUID revokedBy) {
        this.active = false;
        this.revokedAt = LocalDateTime.now();
        this.revokedBy = revokedBy;
    }

    public boolean allowsAction(String action) {
        return this.permission.equals(action) || this.permission.equals("manage");
    }
}
