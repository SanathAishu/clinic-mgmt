package com.hospital.patient.dto;

import com.hospital.common.enums.Disease;
import com.hospital.common.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating a patient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePatientRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    private String phone;

    private Gender gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private Disease disease;

    private String medicalHistory;

    @Size(max = 100, message = "Emergency contact must not exceed 100 characters")
    private String emergencyContact;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Emergency phone must be valid")
    private String emergencyPhone;

    private Boolean active;
}
