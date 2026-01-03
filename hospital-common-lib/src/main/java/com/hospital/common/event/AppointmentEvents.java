package com.hospital.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public final class AppointmentEvents {

    private AppointmentEvents() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Appointment Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class AppointmentCreatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID appointmentId;
        private UUID patientId;
        private UUID doctorId;
        private LocalDateTime appointmentDateTime;
        private String reason;
        private String status;
        private String disease;

        public AppointmentCreatedEvent(String source, String correlationId, String tenantId,
                                     UUID appointmentId, UUID patientId, UUID doctorId,
                                     LocalDateTime appointmentDateTime, String reason,
                                     String status, String disease) {
            super(source, correlationId, tenantId);
            this.appointmentId = appointmentId;
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.appointmentDateTime = appointmentDateTime;
            this.reason = reason;
            this.status = status;
            this.disease = disease;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class AppointmentCancelledEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID appointmentId;
        private UUID patientId;
        private UUID doctorId;
        private String cancellationReason;

        public AppointmentCancelledEvent(String source, String correlationId, String tenantId,
                                       UUID appointmentId, UUID patientId, UUID doctorId,
                                       String cancellationReason) {
            super(source, correlationId, tenantId);
            this.appointmentId = appointmentId;
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.cancellationReason = cancellationReason;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class AppointmentCompletedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID appointmentId;
        private UUID patientId;
        private UUID doctorId;
        private String notes;

        public AppointmentCompletedEvent(String source, String correlationId, String tenantId,
                                       UUID appointmentId, UUID patientId, UUID doctorId,
                                       String notes) {
            super(source, correlationId, tenantId);
            this.appointmentId = appointmentId;
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.notes = notes;
        }
    }
}
