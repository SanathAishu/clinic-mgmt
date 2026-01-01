package com.hospital.appointment.dto;

import com.hospital.common.enums.AppointmentStatus;
import com.hospital.common.enums.Disease;
import com.hospital.common.enums.Specialty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appointment DTO with patient and doctor snapshot data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private UUID patientId;
    private String patientName;
    private String patientEmail;
    private Disease patientDisease;
    private UUID doctorId;
    private String doctorName;
    private String doctorEmail;
    private Specialty doctorSpecialty;
    private LocalDateTime appointmentDate;
    private AppointmentStatus status;
    private String reason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
