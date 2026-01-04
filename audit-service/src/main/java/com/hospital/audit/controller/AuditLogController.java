package com.hospital.audit.controller;

import com.hospital.audit.dto.*;
import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.service.AuditLogService;
import com.hospital.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit log management APIs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new audit log entry")
    public Mono<ApiResponse<AuditLogDto>> createAuditLog(
            @Valid @RequestBody CreateAuditLogRequest request) {
        log.info("Creating audit log: {} - {}", request.getCategory(), request.getAction());
        return auditLogService.createAuditLog(request)
                .map(auditLog -> ApiResponse.success("Audit log created", auditLog));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID")
    public Mono<ApiResponse<AuditLogDto>> getAuditLogById(@PathVariable UUID id) {
        log.info("Fetching audit log by ID: {}", id);
        return auditLogService.getAuditLogById(id)
                .map(ApiResponse::success);
    }

    @GetMapping("/search")
    @Operation(summary = "Search audit logs with criteria")
    public Flux<AuditLogDto> searchAuditLogs(
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean success) {

        AuditSearchCriteria criteria = AuditSearchCriteria.builder()
                .category(category)
                .action(action)
                .serviceName(serviceName)
                .entityType(entityType)
                .entityId(entityId)
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .success(success)
                .build();

        return auditLogService.searchAuditLogs(criteria);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get audit logs by category")
    public Flux<AuditLogDto> getByCategory(@PathVariable AuditCategory category) {
        return auditLogService.getAuditLogsByCategory(category);
    }

    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit logs by action")
    public Flux<AuditLogDto> getByAction(@PathVariable AuditAction action) {
        return auditLogService.getAuditLogsByAction(action);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit logs by user ID")
    public Flux<AuditLogDto> getByUserId(@PathVariable UUID userId) {
        return auditLogService.getAuditLogsByUserId(userId);
    }

    @GetMapping("/service/{serviceName}")
    @Operation(summary = "Get audit logs by service name")
    public Flux<AuditLogDto> getByService(@PathVariable String serviceName) {
        return auditLogService.getAuditLogsByService(serviceName);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get entity change history")
    public Flux<AuditLogDto> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        log.info("Fetching entity history: {} - {}", entityType, entityId);
        return auditLogService.getEntityHistory(entityType, entityId);
    }

    @GetMapping("/user/{userId}/activity")
    @Operation(summary = "Get user activity in date range")
    public Flux<AuditLogDto> getUserActivity(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return auditLogService.getUserActivityInRange(userId, startDate, endDate);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get audit statistics")
    public Mono<ApiResponse<AuditStatistics>> getStatistics(
            @RequestParam(defaultValue = "24") int hoursBack) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        return auditLogService.getStatistics(since)
                .map(ApiResponse::success);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public Mono<String> health() {
        return Mono.just("Audit Service is running");
    }
}
