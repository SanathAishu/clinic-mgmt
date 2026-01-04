package com.hospital.appointment.entity;

import com.hospital.common.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appointment entity with UUID foreign keys using R2DBC
 * Patient and Doctor data denormalized in snapshots
 */
@Table(name = "appointments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    private UUID id;

    // UUID foreign keys - NO database FK constraints
    @Column
    private UUID patientId;

    @Column
    private UUID doctorId;

    @Column
    private LocalDateTime appointmentDate;

    @Column
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column
    private String reason;

    @Column
    private String notes;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // JSONB metadata
    @Column
    private String metadata;
}
