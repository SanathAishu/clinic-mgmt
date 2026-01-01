package com.hospital.medicalrecords.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private LocalDate recordDate;
    private String diagnosis;
    private String symptoms;
    private String treatment;
    private String notes;
    private String bloodPressure;
    private Double temperature;
    private Integer heartRate;
    private Double weight;
    private Double height;
    private PrescriptionDto prescription;
    private MedicalReportDto medicalReport;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
