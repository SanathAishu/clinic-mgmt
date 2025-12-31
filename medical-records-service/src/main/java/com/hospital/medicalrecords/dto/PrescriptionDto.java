package com.hospital.medicalrecords.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDto {
    private UUID id;
    private UUID medicalRecordId;
    private UUID patientId;
    private UUID doctorId;
    private LocalDate prescriptionDate;
    private String medications;
    private String dosageInstructions;
    private Integer durationDays;
    private String specialInstructions;
    private Boolean refillable;
    private Integer refillsRemaining;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
