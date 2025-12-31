package com.hospital.medicalrecords.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMedicalRecordRequest {
    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Doctor ID is required")
    private UUID doctorId;

    @NotNull(message = "Record date is required")
    @PastOrPresent(message = "Record date cannot be in the future")
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
}
