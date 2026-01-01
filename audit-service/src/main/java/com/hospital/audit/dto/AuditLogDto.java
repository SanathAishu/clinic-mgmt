package com.hospital.audit.dto;

import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {
    private UUID id;
    private AuditCategory category;
    private AuditAction action;
    private String serviceName;
    private String entityType;
    private String entityId;
    private UUID userId;
    private String userEmail;
    private String userRole;
    private String description;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String requestId;
    private String correlationId;
    private String metadata;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime timestamp;
}
