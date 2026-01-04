package com.hospital.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Role;

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
 * User entity with UUID primary key for authentication
 */
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private UUID id;

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

    @CreatedDate
    @Column
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Metadata stored as JSONB in PostgreSQL
    @Column
    private String metadata;
}
