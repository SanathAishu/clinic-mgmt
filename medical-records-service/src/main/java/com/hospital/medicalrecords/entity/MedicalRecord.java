package com.hospital.medicalrecords.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

    @OneToOne(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private Prescription prescription;

    @OneToOne(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private MedicalReport medicalReport;

    @Column
    private Boolean active = true;

    @CreatedDate
    @Column
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
