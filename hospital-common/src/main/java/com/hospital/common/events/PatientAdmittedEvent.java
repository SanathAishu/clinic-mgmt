package com.hospital.common.events;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PatientAdmittedEvent extends BaseEvent {
    private UUID bookingId;
    private UUID patientId;
    private UUID roomId;
    private String roomNumber;
    private LocalDate admissionDate;
}
