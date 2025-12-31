package com.hospital.patient.dto;

import com.hospital.common.enums.Disease;
import com.hospital.common.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a patient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    private String phone;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotNull(message = "Disease is required")
    private Disease disease;

    private String medicalHistory;

    @Size(max = 100, message = "Emergency contact must not exceed 100 characters")
    private String emergencyContact;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Emergency phone must be valid")
    private String emergencyPhone;
}
