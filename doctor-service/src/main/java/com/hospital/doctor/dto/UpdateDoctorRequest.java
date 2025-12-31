package com.hospital.doctor.dto;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Specialty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a doctor
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDoctorRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    private String phone;

    private Gender gender;

    private Specialty specialty;

    @Size(max = 50, message = "License number must not exceed 50 characters")
    private String licenseNumber;

    @Min(value = 0, message = "Years of experience must be at least 0")
    @Max(value = 70, message = "Years of experience must not exceed 70")
    private Integer yearsOfExperience;

    @Size(max = 500, message = "Qualifications must not exceed 500 characters")
    private String qualifications;

    private String biography;

    @Size(max = 100, message = "Clinic address must not exceed 100 characters")
    private String clinicAddress;

    private String consultationFee;

    private Boolean available;

    private Boolean active;
}
