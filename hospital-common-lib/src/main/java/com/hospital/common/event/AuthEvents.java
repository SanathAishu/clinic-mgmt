package com.hospital.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Auth Service Domain Events
 */
public final class AuthEvents {

    private AuthEvents() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== User Registration Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegisteredEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID userId;
        private String email;
        private String name;
        private String role;

        public UserRegisteredEvent(String source, String correlationId, String tenantId,
                                 UUID userId, String email, String name, String role) {
            super(source, correlationId, tenantId);
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.role = role;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class UserLoggedInEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID userId;
        private String email;
        private String ipAddress;

        public UserLoggedInEvent(String source, String correlationId, String tenantId,
                               UUID userId, String email, String ipAddress) {
            super(source, correlationId, tenantId);
            this.userId = userId;
            this.email = email;
            this.ipAddress = ipAddress;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class PasswordChangedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID userId;
        private String email;

        public PasswordChangedEvent(String source, String correlationId, String tenantId,
                                  UUID userId, String email) {
            super(source, correlationId, tenantId);
            this.userId = userId;
            this.email = email;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserUpdatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID userId;
        private java.util.Map<String, Object> changes;

        public UserUpdatedEvent(String source, String correlationId, String tenantId,
                              UUID userId, java.util.Map<String, Object> changes) {
            super(source, correlationId, tenantId);
            this.userId = userId;
            this.changes = changes;
        }
    }
}
