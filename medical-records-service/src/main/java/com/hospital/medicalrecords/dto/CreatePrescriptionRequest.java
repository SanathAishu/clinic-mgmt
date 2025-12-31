package com.hospital.medicalrecords.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePrescriptionRequest {
    @NotNull(message = "Medical record ID is required")
    private UUID medicalRecordId;

    @NotNull(message = "Prescription date is required")
    @PastOrPresent(message = "Prescription date cannot be in the future")
    private LocalDate prescriptionDate;

    @NotBlank(message = "Medications are required")
    private String medications;

    private String dosageInstructions;

    @PositiveOrZero(message = "Duration days must be positive or zero")
    private Integer durationDays;

    private String specialInstructions;

    private Boolean refillable = false;

    @PositiveOrZero(message = "Refills remaining must be positive or zero")
    private Integer refillsRemaining = 0;
}
