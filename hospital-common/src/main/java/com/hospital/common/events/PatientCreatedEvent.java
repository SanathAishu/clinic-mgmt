package com.hospital.common.events;

import com.hospital.common.enums.Disease;
import com.hospital.common.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Event published when a new patient is created
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PatientCreatedEvent extends BaseEvent {
    private UUID patientId;
    private String name;
    private String email;
    private String phone;
    private Gender gender;
    private Disease disease;
}
