package com.hospital.doctor.entity;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Specialty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Doctor entity with UUID primary key using R2DBC
 * No cascade relationships - managed via REST APIs
 */
@Table(name = "doctors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Column
    private UUID userId;  // Reference to User in Auth Service

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
    private String licenseNumber;

    @Column
    private Integer yearsOfExperience;

    @Column
    private String qualifications;

    @Column
    private String biography;

    @Column
    private String clinicAddress;

    @Column
    private String consultationFee;

    @Builder.Default
    private Boolean available = true;

    @Builder.Default
    private Boolean active = true;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // JSONB metadata for flexible storage (e.g., working hours, languages)
    @Column
    private String metadata;

    @Override
    public boolean isNew() {
        return isNew;
    }
}
