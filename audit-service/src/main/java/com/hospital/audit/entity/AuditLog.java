package com.hospital.audit.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private UUID id;

    @Column
    private AuditCategory category;

    @Column
    private AuditAction action;

    @Column
    private String serviceName;

    @Column
    private String entityType;

    @Column
    private String entityId;

    @Column
    private UUID userId;

    @Column
    private String userEmail;

    @Column
    private String userRole;

    @Column
    private String description;

    @Column
    private String oldValue;

    @Column
    private String newValue;

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    @Column
    private String requestId;

    @Column
    private String correlationId;

    @Column
    private String metadata;

    @Column
    private Boolean success;

    @Column
    private String errorMessage;

    @Column
    private LocalDateTime timestamp;
}
