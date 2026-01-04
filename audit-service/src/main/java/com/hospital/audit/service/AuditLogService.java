package com.hospital.audit.service;

import com.hospital.audit.dto.*;
import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.entity.AuditLog;
import com.hospital.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public Mono<AuditLogDto> createAuditLog(CreateAuditLogRequest request) {
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

        return auditLogRepository.save(auditLog)
                .map(this::mapToDto)
                .doOnSuccess(dto -> log.info("Audit log created with ID: {}", dto.getId()));
    }

    public Mono<AuditLogDto> getAuditLogById(UUID id) {
        log.debug("Fetching audit log by ID: {}", id);
        return auditLogRepository.findById(id)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Audit log not found: " + id)));
    }

    public Flux<AuditLogDto> searchAuditLogs(AuditSearchCriteria criteria) {
        log.debug("Searching audit logs with criteria: {}", criteria);
        return auditLogRepository.findAll()
                .map(this::mapToDto);
    }

    public Flux<AuditLogDto> getAuditLogsByCategory(AuditCategory category) {
        log.debug("Fetching audit logs by category: {}", category);
        return auditLogRepository.findByCategory(category)
                .map(this::mapToDto);
    }

    public Flux<AuditLogDto> getAuditLogsByAction(AuditAction action) {
        log.debug("Fetching audit logs by action: {}", action);
        return auditLogRepository.findByAction(action)
                .map(this::mapToDto);
    }

    public Flux<AuditLogDto> getAuditLogsByUserId(UUID userId) {
        log.debug("Fetching audit logs for user: {}", userId);
        return auditLogRepository.findByUserId(userId)
                .map(this::mapToDto);
    }

    public Flux<AuditLogDto> getAuditLogsByService(String serviceName) {
        log.debug("Fetching audit logs for service: {}", serviceName);
        return auditLogRepository.findByServiceName(serviceName)
                .map(this::mapToDto);
    }

    public Flux<AuditLogDto> getEntityHistory(String entityType, String entityId) {
        log.debug("Fetching entity history: {} - {}", entityType, entityId);
        return auditLogRepository.findEntityHistory(entityType, entityId)
                .map(this::mapToDto);
    }

    public Flux<AuditLogDto> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching audit logs between {} and {}", startDate, endDate);
        return auditLogRepository.findByDateRange(startDate, endDate)
                .map(this::mapToDto);
    }

    public Flux<AuditLogDto> getUserActivityInRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching user activity for {} between {} and {}", userId, startDate, endDate);
        return auditLogRepository.findUserActivityInRange(userId, startDate, endDate)
                .map(this::mapToDto);
    }

    public Mono<AuditStatistics> getStatistics(LocalDateTime since) {
        log.debug("Generating audit statistics since: {}", since);

        Mono<Long> totalLogsMono = auditLogRepository.count();
        Mono<Long> successCountMono = auditLogRepository.countBySuccess(true);
        Mono<Long> failureCountMono = auditLogRepository.countBySuccess(false);

        Mono<Map<String, Long>> byCategoryMono = auditLogRepository.countByCategory(since)
                .collectList()
                .map(rows -> {
                    Map<String, Long> byCategory = new HashMap<>();
                    rows.forEach(row -> byCategory.put(((AuditCategory) row[0]).name(), (Long) row[1]));
                    return byCategory;
                });

        Mono<Map<String, Long>> byActionMono = auditLogRepository.countByAction(since)
                .collectList()
                .map(rows -> {
                    Map<String, Long> byAction = new HashMap<>();
                    rows.forEach(row -> byAction.put(((AuditAction) row[0]).name(), (Long) row[1]));
                    return byAction;
                });

        Mono<Map<String, Long>> byServiceMono = auditLogRepository.countByService(since)
                .collectList()
                .map(rows -> {
                    Map<String, Long> byService = new HashMap<>();
                    rows.forEach(row -> byService.put((String) row[0], (Long) row[1]));
                    return byService;
                });

        return Mono.zip(totalLogsMono, successCountMono, failureCountMono, byCategoryMono, byActionMono, byServiceMono)
                .map(tuple -> AuditStatistics.builder()
                        .totalLogs(tuple.getT1())
                        .successCount(tuple.getT2())
                        .failureCount(tuple.getT3())
                        .byCategory(tuple.getT4())
                        .byAction(tuple.getT5())
                        .byService(tuple.getT6())
                        .build());
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
