package com.hospital.patient.entity;

import com.hospital.common.enums.Disease;
import com.hospital.common.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Patient entity with UUID primary key using R2DBC
 * No cascade relationships - managed via REST APIs
 */
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient implements Persistable<UUID> {

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
    private LocalDate dateOfBirth;

    @Column
    private String address;

    @Column
    private Disease disease;

    @Column
    private String medicalHistory;

    @Column
    private String emergencyContact;

    @Column
    private String emergencyPhone;

    @Builder.Default
    private Boolean active = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // JSONB metadata for flexible storage
    @Column
    private String metadata;

    // Calculate age from date of birth
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
