package com.hospital.common.events;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Specialty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Event published when a new doctor is created
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DoctorCreatedEvent extends BaseEvent {
    private UUID doctorId;
    private String name;
    private String email;
    private String phone;
    private Gender gender;
    private Specialty specialty;
}
