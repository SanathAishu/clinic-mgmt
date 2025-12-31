package com.hospital.appointment.dto;

import com.hospital.common.enums.AppointmentStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for updating an appointment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppointmentRequest {

    @Future(message = "Appointment date must be in the future")
    private LocalDateTime appointmentDate;

    private AppointmentStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    private String notes;
}
