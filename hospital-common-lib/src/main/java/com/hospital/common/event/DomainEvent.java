package com.hospital.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Provides common properties for tracking and correlation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DomainEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private LocalDateTime timestamp;
    private String source;  // Which service published this event
    private String correlationId;  // For tracing related operations
    private String tenantId;  // Multi-tenancy: Which hospital/clinic this event belongs to

    public DomainEvent(String source, String correlationId, String tenantId) {
        this.eventId = UUID.randomUUID();
        this.timestamp = LocalDateTime.now();
        this.source = source;
        this.correlationId = correlationId;
        this.tenantId = tenantId;
    }

    // Legacy constructor for backward compatibility (deprecated)
    @Deprecated
    public DomainEvent(String source, String correlationId) {
        this(source, correlationId, null);
    }
}
