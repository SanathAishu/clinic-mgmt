package com.hospital.auth.event;

import com.hospital.common.event.Auditable;
import com.hospital.common.event.EventMetadata;
import com.hospital.common.event.TenantAware;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a new user registers.
 *
 * Architecture:
 * - Uses composition (EventMetadata) instead of inheritance
 * - Implements interfaces (TenantAware, Auditable) for contracts
 * - Immutable with final fields
 *
 * Consumers:
 * - Notification Service (send welcome email)
 * - Audit Service (log user creation)
 */
@RegisterForReflection // Required for native compilation
public class UserRegisteredEvent implements TenantAware, Auditable {

    // Composition: Embed metadata instead of extending base class
    private final EventMetadata metadata;

    // Tenant context (CRITICAL for event isolation)
    private final String tenantId;

    // Business data
    private final UUID userId;
    private final String name;
    private final String email;
    private final String phone;

    public UserRegisteredEvent(String tenantId, UUID userId, String name,
                               String email, String phone) {
        this.metadata = new EventMetadata("UserRegistered");
        this.tenantId = tenantId;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // Interface implementations

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public UUID getEventId() {
        return metadata.getEventId();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return metadata.getOccurredAt();
    }

    // Business getters

    public String getEventType() {
        return metadata.getEventType();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public EventMetadata getMetadata() {
        return metadata;
    }
}
