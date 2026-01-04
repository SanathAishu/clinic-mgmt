package com.hospital.medicalrecords.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "medical_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalReport {

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
    private String reportType;

    @Column
    private LocalDate reportDate;

    @Column
    private String findings;

    @Column
    private String conclusion;

    @Column
    private String recommendations;

    private String labName;

    private String technicianName;

    @Column
    private Boolean active = true;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
