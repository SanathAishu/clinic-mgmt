package com.hospital.facility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmitPatientRequest {
    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Doctor ID is required")
    private UUID doctorId;

    private LocalDate admissionDate;

    private String admissionReason;

    private String notes;
}
