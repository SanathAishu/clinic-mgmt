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
public class MedicalReportDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private UUID medicalRecordId;
    private UUID patientId;
    private UUID doctorId;
    private String reportType;
    private LocalDate reportDate;
    private String findings;
    private String conclusion;
    private String recommendations;
    private String labName;
    private String technicianName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
