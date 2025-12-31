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
public class PrescriptionCreatedEvent extends BaseEvent {
    private UUID prescriptionId;
    private UUID medicalRecordId;
    private UUID patientId;
    private UUID doctorId;
    private String medications;
    private LocalDate prescriptionDate;
}
