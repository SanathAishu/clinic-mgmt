package com.hospital.medicalrecords.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    private UUID id;

    @Column
    private UUID patientId;

    @Column
    private UUID doctorId;

    @Column
    private LocalDate recordDate;

    @Column
    private String diagnosis;

    @Column
    private String symptoms;

    @Column
    private String treatment;

    @Column
    private String notes;

    private String bloodPressure;

    private Double temperature;

    private Integer heartRate;

    private Double weight;

    private Double height;

    @org.springframework.data.annotation.Transient
    private Prescription prescription;

    @org.springframework.data.annotation.Transient
    private MedicalReport medicalReport;

    @Column
    private Boolean active = true;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
