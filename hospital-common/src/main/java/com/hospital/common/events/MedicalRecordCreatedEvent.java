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
public class MedicalRecordCreatedEvent extends BaseEvent {
    private UUID medicalRecordId;
    private UUID patientId;
    private UUID doctorId;
    private String diagnosis;
    private LocalDate recordDate;
}
