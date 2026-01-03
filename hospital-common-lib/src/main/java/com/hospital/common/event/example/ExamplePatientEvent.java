package com.hospital.common.event.example;

import com.hospital.common.event.Auditable;
import com.hospital.common.event.EventMetadata;
import com.hospital.common.event.TenantAware;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Example event demonstrating composition-based design.
 *
 * DESIGN PATTERN: Composition + Interfaces (NO inheritance)
 *
 * Key principles:
 * 1. Implements interfaces (TenantAware, Auditable) instead of extending base class
 * 2. Embeds EventMetadata via composition instead of inheriting fields
 * 3. Immutable (final fields, no setters)
 * 4. Constructor-only initialization
 *
 * Benefits:
 * - No fragile base class problem
 * - Can implement multiple contracts without diamond problem
 * - Easy to test (mock interfaces, not abstract classes)
 * - Clear separation of concerns
 */
@RegisterForReflection // Required for native compilation (RabbitMQ serialization)
public class ExamplePatientEvent implements TenantAware, Auditable {

    // Composition: Embed metadata instead of inheriting from base class
    private final EventMetadata metadata;

    // Business data (immutable)
    private final String tenantId;
    private final UUID patientId;
    private final String patientName;
    private final String action; // e.g., "CREATED", "UPDATED", "DELETED"

    public ExamplePatientEvent(String tenantId, UUID patientId,
                               String patientName, String action) {
        // Validate inputs (fail-fast)
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (patientId == null) {
            throw new IllegalArgumentException("patientId is required");
        }

        // Initialize metadata via composition
        this.metadata = new EventMetadata("PatientEvent:" + action);

        // Initialize business data
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.action = action;
    }

    // ======================================================================
    // Interface implementations (delegate to metadata or return own fields)
    // ======================================================================

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

    // ======================================================================
    // Business getters (NO setters - immutable)
    // ======================================================================

    public UUID getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return String.format("ExamplePatientEvent{tenantId=%s, patientId=%s, action=%s, %s}",
                tenantId, patientId, action, metadata);
    }
}
