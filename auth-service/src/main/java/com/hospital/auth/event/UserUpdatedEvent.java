package com.hospital.auth.event;

import com.hospital.common.event.Auditable;
import com.hospital.common.event.EventMetadata;
import com.hospital.common.event.TenantAware;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when user profile is updated.
 *
 * Architecture:
 * - Uses composition (EventMetadata) instead of inheritance
 * - Implements interfaces (TenantAware, Auditable) for contracts
 * - Immutable with final fields
 *
 * Consumers:
 * - Audit Service (log changes)
 * - Cache Service (invalidate user cache)
 */
@RegisterForReflection // Required for native compilation
public class UserUpdatedEvent implements TenantAware, Auditable {

    // Composition: Embed metadata instead of extending base class
    private final EventMetadata metadata;

    // Tenant context
    private final String tenantId;

    // Business data
    private final UUID userId;
    private final Map<String, Object> changes; // Field name -> new value

    public UserUpdatedEvent(String tenantId, UUID userId, Map<String, Object> changes) {
        this.metadata = new EventMetadata("UserUpdated");
        this.tenantId = tenantId;
        this.userId = userId;
        this.changes = changes;
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

    public Map<String, Object> getChanges() {
        return changes;
    }

    public EventMetadata getMetadata() {
        return metadata;
    }
}
