package com.hospital.appointment.entity;

import com.hospital.common.enums.Disease;
import com.hospital.common.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Patient snapshot - denormalized patient data for appointments
 * Updated via REST API calls from Patient Service
 */
@Table(name = "patient_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientSnapshot {

    @Id
    private UUID patientId;  // Same as Patient.id in Patient Service

    @Column
    private String name;

    @Column
    private String email;

    @Column
    private String phone;

    @Column
    private Gender gender;

    @Column
    private Disease disease;

    @LastModifiedDate
    private LocalDateTime lastUpdated;
}
