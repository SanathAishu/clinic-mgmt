package com.hospital.audit.service;

import com.hospital.audit.dto.*;
import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.entity.AuditLog;
import com.hospital.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLogDto createAuditLog(CreateAuditLogRequest request) {
        log.debug("Creating audit log: {} - {} - {}", request.getCategory(), request.getAction(), request.getServiceName());

        AuditLog auditLog = AuditLog.builder()
                .category(request.getCategory())
                .action(request.getAction())
                .serviceName(request.getServiceName())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .userId(request.getUserId())
                .userEmail(request.getUserEmail())
                .userRole(request.getUserRole())
                .description(request.getDescription())
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .requestId(request.getRequestId())
                .correlationId(request.getCorrelationId())
                .metadata(request.getMetadata())
                .success(request.getSuccess() != null ? request.getSuccess() : true)
                .errorMessage(request.getErrorMessage())
                .build();

        auditLog = auditLogRepository.save(auditLog);
        log.info("Audit log created with ID: {}", auditLog.getId());

        return mapToDto(auditLog);
    }

    public AuditLogDto getAuditLogById(UUID id) {
        log.debug("Fetching audit log by ID: {}", id);
        return auditLogRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Audit log not found: " + id));
    }

    public Page<AuditLogDto> searchAuditLogs(AuditSearchCriteria criteria, Pageable pageable) {
        log.debug("Searching audit logs with criteria: {}", criteria);
        return auditLogRepository.findAll(AuditLogSpecification.withCriteria(criteria), pageable)
                .map(this::mapToDto);
    }

    public Page<AuditLogDto> getAuditLogsByCategory(AuditCategory category, Pageable pageable) {
        log.debug("Fetching audit logs by category: {}", category);
        return auditLogRepository.findByCategory(category, pageable)
                .map(this::mapToDto);
    }

    public Page<AuditLogDto> getAuditLogsByAction(AuditAction action, Pageable pageable) {
        log.debug("Fetching audit logs by action: {}", action);
        return auditLogRepository.findByAction(action, pageable)
                .map(this::mapToDto);
    }

    public Page<AuditLogDto> getAuditLogsByUserId(UUID userId, Pageable pageable) {
        log.debug("Fetching audit logs for user: {}", userId);
        return auditLogRepository.findByUserId(userId, pageable)
                .map(this::mapToDto);
    }

    public Page<AuditLogDto> getAuditLogsByService(String serviceName, Pageable pageable) {
        log.debug("Fetching audit logs for service: {}", serviceName);
        return auditLogRepository.findByServiceName(serviceName, pageable)
                .map(this::mapToDto);
    }

    public List<AuditLogDto> getEntityHistory(String entityType, String entityId) {
        log.debug("Fetching entity history: {} - {}", entityType, entityId);
        return auditLogRepository.findEntityHistory(entityType, entityId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public Page<AuditLogDto> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Fetching audit logs between {} and {}", startDate, endDate);
        return auditLogRepository.findByDateRange(startDate, endDate, pageable)
                .map(this::mapToDto);
    }

    public Page<AuditLogDto> getUserActivityInRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Fetching user activity for {} between {} and {}", userId, startDate, endDate);
        return auditLogRepository.findUserActivityInRange(userId, startDate, endDate, pageable)
                .map(this::mapToDto);
    }

    public AuditStatistics getStatistics(LocalDateTime since) {
        log.debug("Generating audit statistics since: {}", since);

        long totalLogs = auditLogRepository.count();
        long successCount = auditLogRepository.countBySuccess(true);
        long failureCount = auditLogRepository.countBySuccess(false);

        Map<String, Long> byCategory = new HashMap<>();
        auditLogRepository.countByCategory(since).forEach(row ->
            byCategory.put(((AuditCategory) row[0]).name(), (Long) row[1])
        );

        Map<String, Long> byAction = new HashMap<>();
        auditLogRepository.countByAction(since).forEach(row ->
            byAction.put(((AuditAction) row[0]).name(), (Long) row[1])
        );

        Map<String, Long> byService = new HashMap<>();
        auditLogRepository.countByService(since).forEach(row ->
            byService.put((String) row[0], (Long) row[1])
        );

        return AuditStatistics.builder()
                .totalLogs(totalLogs)
                .successCount(successCount)
                .failureCount(failureCount)
                .byCategory(byCategory)
                .byAction(byAction)
                .byService(byService)
                .build();
    }

    private AuditLogDto mapToDto(AuditLog auditLog) {
        return AuditLogDto.builder()
                .id(auditLog.getId())
                .category(auditLog.getCategory())
                .action(auditLog.getAction())
                .serviceName(auditLog.getServiceName())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .userId(auditLog.getUserId())
                .userEmail(auditLog.getUserEmail())
                .userRole(auditLog.getUserRole())
                .description(auditLog.getDescription())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .requestId(auditLog.getRequestId())
                .correlationId(auditLog.getCorrelationId())
                .metadata(auditLog.getMetadata())
                .success(auditLog.getSuccess())
                .errorMessage(auditLog.getErrorMessage())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
