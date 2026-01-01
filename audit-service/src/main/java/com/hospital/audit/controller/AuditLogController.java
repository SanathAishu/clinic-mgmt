package com.hospital.audit.controller;

import com.hospital.audit.dto.*;
import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.service.AuditLogService;
import com.hospital.common.dto.ApiResponse;
import com.hospital.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit log management APIs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping
    @Operation(summary = "Create a new audit log entry")
    public ResponseEntity<ApiResponse<AuditLogDto>> createAuditLog(
            @Valid @RequestBody CreateAuditLogRequest request) {
        log.info("Creating audit log: {} - {}", request.getCategory(), request.getAction());
        AuditLogDto auditLog = auditLogService.createAuditLog(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Audit log created", auditLog));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID")
    public ResponseEntity<ApiResponse<AuditLogDto>> getAuditLogById(@PathVariable UUID id) {
        log.info("Fetching audit log by ID: {}", id);
        AuditLogDto auditLog = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success(auditLog));
    }

    @GetMapping("/search")
    @Operation(summary = "Search audit logs with criteria")
    public ResponseEntity<PageResponse<AuditLogDto>> searchAuditLogs(
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean success,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

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

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogDto> auditLogs = auditLogService.searchAuditLogs(criteria, pageable);

        return ResponseEntity.ok(buildPageResponse(auditLogs));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get audit logs by category")
    public ResponseEntity<PageResponse<AuditLogDto>> getByCategory(
            @PathVariable AuditCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByCategory(category, pageable);
        return ResponseEntity.ok(buildPageResponse(auditLogs));
    }

    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit logs by action")
    public ResponseEntity<PageResponse<AuditLogDto>> getByAction(
            @PathVariable AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByAction(action, pageable);
        return ResponseEntity.ok(buildPageResponse(auditLogs));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit logs by user ID")
    public ResponseEntity<PageResponse<AuditLogDto>> getByUserId(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByUserId(userId, pageable);
        return ResponseEntity.ok(buildPageResponse(auditLogs));
    }

    @GetMapping("/service/{serviceName}")
    @Operation(summary = "Get audit logs by service name")
    public ResponseEntity<PageResponse<AuditLogDto>> getByService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByService(serviceName, pageable);
        return ResponseEntity.ok(buildPageResponse(auditLogs));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get entity change history")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        log.info("Fetching entity history: {} - {}", entityType, entityId);
        List<AuditLogDto> history = auditLogService.getEntityHistory(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/user/{userId}/activity")
    @Operation(summary = "Get user activity in date range")
    public ResponseEntity<PageResponse<AuditLogDto>> getUserActivity(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogDto> auditLogs = auditLogService.getUserActivityInRange(userId, startDate, endDate, pageable);
        return ResponseEntity.ok(buildPageResponse(auditLogs));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get audit statistics")
    public ResponseEntity<ApiResponse<AuditStatistics>> getStatistics(
            @RequestParam(defaultValue = "24") int hoursBack) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        AuditStatistics statistics = auditLogService.getStatistics(since);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Audit Service is running");
    }

    private PageResponse<AuditLogDto> buildPageResponse(Page<AuditLogDto> page) {
        return PageResponse.<AuditLogDto>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
