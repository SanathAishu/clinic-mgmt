package com.hospital.appointment.entity;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Specialty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Doctor snapshot - denormalized doctor data for appointments
 * Updated via REST API calls from Doctor Service
 */
@Table(name = "doctor_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSnapshot {

    @Id
    private UUID doctorId;  // Same as Doctor.id in Doctor Service

    @Column
    private String name;

    @Column
    private String email;

    @Column
    private String phone;

    @Column
    private Gender gender;

    @Column
    private Specialty specialty;

    @Column
    private LocalDateTime lastUpdated;
}
