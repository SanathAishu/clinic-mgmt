package com.hospital.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Audit Service Domain Events
 */
public final class AuditEvents {

    private AuditEvents() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Audit Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class AuditLogEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID auditId;
        private UUID userId;
        private String action;
        private String entityType;
        private UUID entityId;
        private String details;
        private String status;
        private String ipAddress;

        public AuditLogEvent(String source, String correlationId, String tenantId,
                           UUID auditId, UUID userId, String action,
                           String entityType, UUID entityId, String details,
                           String status, String ipAddress) {
            super(source, correlationId, tenantId);
            this.auditId = auditId;
            this.userId = userId;
            this.action = action;
            this.entityType = entityType;
            this.entityId = entityId;
            this.details = details;
            this.status = status;
            this.ipAddress = ipAddress;
        }
    }
}
