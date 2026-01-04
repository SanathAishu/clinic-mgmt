package com.hospital.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity with UUID primary key for authentication
 */
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Column
    private String email;

    @Column
    private String password;  // BCrypt hashed

    @Column
    private String name;

    @Column
    private String phone;

    @Column
    private Gender gender;

    @Column
    private Role role;

    @Column
    @Builder.Default
    private Boolean active = true;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Metadata stored as JSONB in PostgreSQL
    @Column
    private String metadata;

    @Override
    public boolean isNew() {
        return isNew;
    }
}
