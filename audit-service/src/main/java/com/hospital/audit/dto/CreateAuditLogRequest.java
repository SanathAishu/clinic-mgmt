package com.hospital.audit.dto;

import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAuditLogRequest {

    @NotNull(message = "Category is required")
    private AuditCategory category;

    @NotNull(message = "Action is required")
    private AuditAction action;

    @NotNull(message = "Service name is required")
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
    private String userAgent;
    private String requestId;
    private String correlationId;
    private String metadata;

    @Builder.Default
    private Boolean success = true;

    private String errorMessage;
}
