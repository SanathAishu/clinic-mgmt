package com.hospital.medicalrecords.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateMedicalReportRequest {
    @NotNull(message = "Medical record ID is required")
    private UUID medicalRecordId;

    @NotBlank(message = "Report type is required")
    private String reportType;

    @NotNull(message = "Report date is required")
    @PastOrPresent(message = "Report date cannot be in the future")
    private LocalDate reportDate;

    private String findings;
    private String conclusion;
    private String recommendations;
    private String labName;
    private String technicianName;
}
