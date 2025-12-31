package com.hospital.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an appointment is cancelled
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppointmentCancelledEvent extends BaseEvent {
    private UUID appointmentId;
    private UUID patientId;
    private String patientEmail;
    private UUID doctorId;
    private String doctorEmail;
    private LocalDateTime appointmentDate;
    private String cancellationReason;
}
