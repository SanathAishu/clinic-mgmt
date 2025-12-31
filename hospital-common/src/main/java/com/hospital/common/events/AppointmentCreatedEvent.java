package com.hospital.common.events;

import com.hospital.common.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a new appointment is created
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppointmentCreatedEvent extends BaseEvent {
    private UUID appointmentId;
    private UUID patientId;
    private String patientName;
    private String patientEmail;
    private UUID doctorId;
    private String doctorName;
    private String doctorEmail;
    private LocalDateTime appointmentDate;
    private AppointmentStatus status;
}
