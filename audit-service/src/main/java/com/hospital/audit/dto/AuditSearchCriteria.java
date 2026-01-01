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
public class AuditSearchCriteria {
    private AuditCategory category;
    private AuditAction action;
    private String serviceName;
    private String entityType;
    private String entityId;
    private UUID userId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean success;
}
