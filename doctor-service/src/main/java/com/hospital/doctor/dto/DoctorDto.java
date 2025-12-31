package com.hospital.doctor.dto;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Specialty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Doctor DTO for responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDto {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private Gender gender;
    private Specialty specialty;
    private String licenseNumber;
    private Integer yearsOfExperience;
    private String qualifications;
    private String biography;
    private String clinicAddress;
    private String consultationFee;
    private Boolean available;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
