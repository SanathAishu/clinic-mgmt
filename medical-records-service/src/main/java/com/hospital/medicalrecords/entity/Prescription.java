package com.hospital.medicalrecords.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    private UUID id;

    @Column(value = "medical_record_id")
    private UUID medicalRecordId;

    @org.springframework.data.annotation.Transient
    private MedicalRecord medicalRecord;

    @Column
    private UUID patientId;

    @Column
    private UUID doctorId;

    @Column
    private LocalDate prescriptionDate;

    @Column
    private String medications;

    @Column
    private String dosageInstructions;

    private Integer durationDays;

    @Column
    private String specialInstructions;

    private Boolean refillable = false;

    private Integer refillsRemaining = 0;

    @Column
    private Boolean active = true;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
