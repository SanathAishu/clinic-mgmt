package com.hospital.patient.dto;

import com.hospital.common.enums.Disease;
import com.hospital.common.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Patient DTO for responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDto {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private Gender gender;
    private LocalDate dateOfBirth;
    private Integer age;
    private String address;
    private Disease disease;
    private String medicalHistory;
    private String emergencyContact;
    private String emergencyPhone;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
