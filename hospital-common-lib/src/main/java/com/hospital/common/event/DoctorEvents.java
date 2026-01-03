package com.hospital.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Doctor Service Domain Events
 */
public final class DoctorEvents {

    private DoctorEvents() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Doctor Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class DoctorCreatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID doctorId;
        private String name;
        private String email;
        private String phone;
        private String specialty;
        private String licenseNumber;
        private String qualifications;

        public DoctorCreatedEvent(String source, String correlationId, String tenantId,
                                UUID doctorId, String name, String email, String phone,
                                String specialty, String licenseNumber, String qualifications) {
            super(source, correlationId, tenantId);
            this.doctorId = doctorId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.specialty = specialty;
            this.licenseNumber = licenseNumber;
            this.qualifications = qualifications;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class DoctorUpdatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID doctorId;
        private String name;
        private String email;
        private String phone;
        private String specialty;
        private String qualifications;

        public DoctorUpdatedEvent(String source, String correlationId, String tenantId,
                                UUID doctorId, String name, String email, String phone,
                                String specialty, String qualifications) {
            super(source, correlationId, tenantId);
            this.doctorId = doctorId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.specialty = specialty;
            this.qualifications = qualifications;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class DoctorDeletedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID doctorId;
        private String name;

        public DoctorDeletedEvent(String source, String correlationId, String tenantId,
                                UUID doctorId, String name) {
            super(source, correlationId, tenantId);
            this.doctorId = doctorId;
            this.name = name;
        }
    }
}
