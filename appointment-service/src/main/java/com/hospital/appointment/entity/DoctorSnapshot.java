package com.hospital.appointment.entity;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Specialty;
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
 * Doctor snapshot - denormalized doctor data for appointments
 * Updated via events from Doctor Service
 */
@Entity
@Table(name = "doctor_snapshots")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSnapshot {

    @Id
    private UUID doctorId;  // Same as Doctor.id in Doctor Service

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
    private Specialty specialty;

    @LastModifiedDate
    private LocalDateTime lastUpdated;
}
