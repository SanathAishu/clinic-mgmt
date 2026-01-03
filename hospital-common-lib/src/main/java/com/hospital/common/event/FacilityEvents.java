package com.hospital.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Facility Service Domain Events (includes Saga pattern events for admission)
 */
public final class FacilityEvents {

    private FacilityEvents() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Admission Saga Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class PatientAdmissionRequestedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID admissionId;
        private UUID patientId;
        private String roomType;
        private String reason;
        private LocalDateTime requestedDate;

        public PatientAdmissionRequestedEvent(String source, String correlationId, String tenantId,
                                            UUID admissionId, UUID patientId, String roomType,
                                            String reason, LocalDateTime requestedDate) {
            super(source, correlationId, tenantId);
            this.admissionId = admissionId;
            this.patientId = patientId;
            this.roomType = roomType;
            this.reason = reason;
            this.requestedDate = requestedDate;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class PatientAdmittedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID admissionId;
        private UUID patientId;
        private UUID roomId;
        private String bedNumber;
        private LocalDateTime admissionDateTime;

        public PatientAdmittedEvent(String source, String correlationId, String tenantId,
                                  UUID admissionId, UUID patientId, UUID roomId,
                                  String bedNumber, LocalDateTime admissionDateTime) {
            super(source, correlationId, tenantId);
            this.admissionId = admissionId;
            this.patientId = patientId;
            this.roomId = roomId;
            this.bedNumber = bedNumber;
            this.admissionDateTime = admissionDateTime;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class PatientDischargedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID admissionId;
        private UUID patientId;
        private UUID roomId;
        private LocalDateTime dischargeDateTime;
        private String dischargeNotes;

        public PatientDischargedEvent(String source, String correlationId, String tenantId,
                                    UUID admissionId, UUID patientId, UUID roomId,
                                    LocalDateTime dischargeDateTime, String dischargeNotes) {
            super(source, correlationId, tenantId);
            this.admissionId = admissionId;
            this.patientId = patientId;
            this.roomId = roomId;
            this.dischargeDateTime = dischargeDateTime;
            this.dischargeNotes = dischargeNotes;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class AdmissionFailedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID admissionId;
        private UUID patientId;
        private String failureReason;

        public AdmissionFailedEvent(String source, String correlationId, String tenantId,
                                  UUID admissionId, UUID patientId, String failureReason) {
            super(source, correlationId, tenantId);
            this.admissionId = admissionId;
            this.patientId = patientId;
            this.failureReason = failureReason;
        }
    }

    // ==================== Room Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class RoomCreatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID roomId;
        private String roomNumber;
        private String roomType;
        private int floorNumber;
        private int totalBeds;

        public RoomCreatedEvent(String source, String correlationId, String tenantId,
                              UUID roomId, String roomNumber, String roomType,
                              int floorNumber, int totalBeds) {
            super(source, correlationId, tenantId);
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.floorNumber = floorNumber;
            this.totalBeds = totalBeds;
        }
    }
}
