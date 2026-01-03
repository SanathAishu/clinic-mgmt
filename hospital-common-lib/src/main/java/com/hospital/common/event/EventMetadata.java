package com.hospital.common.event;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event metadata for composition-based event design.
 *
 * DESIGN PRINCIPLE: Composition over Inheritance.
 * Instead of extending a DomainEvent base class, events embed this
 * metadata object. This provides:
 * - Flexibility: Events can implement multiple interfaces
 * - Testability: Easy to mock without abstract class complexity
 * - Maintainability: Change metadata without affecting all events
 *
 * Usage:
 * ```java
 * public class PatientCreatedEvent implements TenantAware, Auditable {
 *     private final EventMetadata metadata;
 *     private final String tenantId;
 *     private final UUID patientId;
 *
 *     public PatientCreatedEvent(String tenantId, UUID patientId) {
 *         this.metadata = new EventMetadata("PatientCreated");
 *         this.tenantId = tenantId;
 *         this.patientId = patientId;
 *     }
 *
 *     @Override
 *     public UUID getEventId() { return metadata.getEventId(); }
 *
 *     @Override
 *     public LocalDateTime getOccurredAt() { return metadata.getOccurredAt(); }
 * }
 * ```
 */
@RegisterForReflection
public class EventMetadata {
    private final UUID eventId;
    private final LocalDateTime occurredAt;
    private final String eventType;

    public EventMetadata(String eventType) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
        this.eventType = eventType;
    }

    public UUID getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return String.format("EventMetadata{eventId=%s, type=%s, occurredAt=%s}",
                eventId, eventType, occurredAt);
    }
}
