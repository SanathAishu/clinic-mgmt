package com.hospital.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

public final class PatientEvents {

    private PatientEvents() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Patient Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class PatientCreatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID patientId;
        private String name;
        private String email;
        private String phone;
        private String gender;
        private LocalDate dateOfBirth;
        private String medicalHistory;

        public PatientCreatedEvent(String source, String correlationId, String tenantId,
                                  UUID patientId, String name, String email, String phone,
                                  String gender, LocalDate dateOfBirth, String medicalHistory) {
            super(source, correlationId, tenantId);
            this.patientId = patientId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.gender = gender;
            this.dateOfBirth = dateOfBirth;
            this.medicalHistory = medicalHistory;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class PatientUpdatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID patientId;
        private String name;
        private String email;
        private String phone;
        private String gender;
        private String medicalHistory;

        public PatientUpdatedEvent(String source, String correlationId, String tenantId,
                                  UUID patientId, String name, String email,
                                  String phone, String gender, String medicalHistory) {
            super(source, correlationId, tenantId);
            this.patientId = patientId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.gender = gender;
            this.medicalHistory = medicalHistory;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class PatientDeletedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID patientId;
        private String name;

        public PatientDeletedEvent(String source, String correlationId, String tenantId,
                                  UUID patientId, String name) {
            super(source, correlationId, tenantId);
            this.patientId = patientId;
            this.name = name;
        }
    }
}
