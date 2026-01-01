package com.hospital.appointment.entity;

import com.hospital.common.enums.Disease;
import com.hospital.common.enums.Gender;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Patient snapshot - denormalized patient data for appointments
 * Updated via events from Patient Service
 */
@Entity
@Table(name = "patient_snapshots")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientSnapshot {

    @Id
    private UUID patientId;  // Same as Patient.id in Patient Service

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Disease disease;

    @LastModifiedDate
    private LocalDateTime lastUpdated;
}
