package com.hospital.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Marker interface for events that provide audit metadata.
 *
 * DESIGN PRINCIPLE: Interface segregation instead of inheritance.
 * Provides contract for events that need to track when and how
 * they occurred, without forcing a specific implementation.
 *
 * Usage:
 * ```java
 * public class PatientCreatedEvent implements TenantAware, Auditable {
 *     private final EventMetadata metadata; // Composition
 *
 *     @Override
 *     public UUID getEventId() { return metadata.getEventId(); }
 *
 *     @Override
 *     public LocalDateTime getOccurredAt() { return metadata.getOccurredAt(); }
 * }
 * ```
 */
public interface Auditable {
    /**
     * Get unique identifier for this event.
     *
     * @return Event UUID (never null)
     */
    UUID getEventId();

    /**
     * Get timestamp when this event occurred.
     *
     * @return Event timestamp (never null)
     */
    LocalDateTime getOccurredAt();
}
