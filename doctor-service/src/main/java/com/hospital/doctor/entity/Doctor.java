package com.hospital.doctor.entity;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Specialty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Doctor entity with UUID primary key
 * No cascade relationships - managed via events
 */
@Entity
@Table(name = "doctors")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;  // Reference to User in Auth Service

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Specialty specialty;

    @Column(length = 50)
    private String licenseNumber;

    @Column(nullable = false)
    private Integer yearsOfExperience;

    @Column(length = 500)
    private String qualifications;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @Column(length = 100)
    private String clinicAddress;

    @Column(length = 20)
    private String consultationFee;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // JSONB metadata for flexible storage (e.g., working hours, languages)
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
