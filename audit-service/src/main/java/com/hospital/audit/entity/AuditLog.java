package com.hospital.audit.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_category", columnList = "category"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_service", columnList = "service_name")
})
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

    @CreatedDate
    @Column
    private LocalDateTime timestamp;
}
