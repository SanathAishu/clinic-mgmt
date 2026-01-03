package com.hospital.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

public final class MedicalRecordEvents {

    private MedicalRecordEvents() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Medical Record Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class MedicalRecordCreatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID recordId;
        private UUID patientId;
        private UUID doctorId;
        private LocalDate recordDate;
        private String diagnosis;
        private String treatment;
        private String notes;

        public MedicalRecordCreatedEvent(String source, String correlationId, String tenantId,
                                       UUID recordId, UUID patientId, UUID doctorId,
                                       LocalDate recordDate, String diagnosis,
                                       String treatment, String notes) {
            super(source, correlationId, tenantId);
            this.recordId = recordId;
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.recordDate = recordDate;
            this.diagnosis = diagnosis;
            this.treatment = treatment;
            this.notes = notes;
        }
    }

    // ==================== Prescription Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class PrescriptionCreatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID prescriptionId;
        private UUID patientId;
        private UUID doctorId;
        private String medicationName;
        private String dosage;
        private String frequency;
        private LocalDate startDate;
        private LocalDate endDate;
        private String instructions;

        public PrescriptionCreatedEvent(String source, String correlationId, String tenantId,
                                      UUID prescriptionId, UUID patientId, UUID doctorId,
                                      String medicationName, String dosage, String frequency,
                                      LocalDate startDate, LocalDate endDate, String instructions) {
            super(source, correlationId, tenantId);
            this.prescriptionId = prescriptionId;
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.medicationName = medicationName;
            this.dosage = dosage;
            this.frequency = frequency;
            this.startDate = startDate;
            this.endDate = endDate;
            this.instructions = instructions;
        }
    }

    // ==================== Medical Report Events ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class MedicalReportCreatedEvent extends DomainEvent {
        private static final long serialVersionUID = 1L;

        private UUID reportId;
        private UUID patientId;
        private String reportType;
        private String findings;
        private String recommendations;
        private LocalDate reportDate;

        public MedicalReportCreatedEvent(String source, String correlationId, String tenantId,
                                       UUID reportId, UUID patientId, String reportType,
                                       String findings, String recommendations, LocalDate reportDate) {
            super(source, correlationId, tenantId);
            this.reportId = reportId;
            this.patientId = patientId;
            this.reportType = reportType;
            this.findings = findings;
            this.recommendations = recommendations;
            this.reportDate = reportDate;
        }
    }
}
