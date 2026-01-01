package com.hospital.audit.dto;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditStatistics {
    private long totalLogs;
    private long successCount;
    private long failureCount;
    private Map<String, Long> byCategory;
    private Map<String, Long> byAction;
    private Map<String, Long> byService;
}
