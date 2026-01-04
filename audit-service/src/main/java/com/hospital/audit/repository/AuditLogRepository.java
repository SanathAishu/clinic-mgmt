package com.hospital.audit.repository;

import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.entity.AuditLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends R2dbcRepository<AuditLog, UUID> {

    Flux<AuditLog> findByCategory(AuditCategory category);

    Flux<AuditLog> findByAction(AuditAction action);

    Flux<AuditLog> findByUserId(UUID userId);

    Flux<AuditLog> findByEntityId(String entityId);

    Flux<AuditLog> findByServiceName(String serviceName);

    @Query("SELECT * FROM audit_logs WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    Flux<AuditLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT * FROM audit_logs WHERE category = :category AND action = :action ORDER BY timestamp DESC")
    Flux<AuditLog> findByCategoryAndAction(
        @Param("category") AuditCategory category,
        @Param("action") AuditAction action
    );

    @Query("SELECT * FROM audit_logs WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY timestamp DESC")
    Flux<AuditLog> findEntityHistory(
        @Param("entityType") String entityType,
        @Param("entityId") String entityId
    );

    @Query("SELECT * FROM audit_logs WHERE user_id = :userId AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    Flux<AuditLog> findUserActivityInRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT category, COUNT(*) as count FROM audit_logs WHERE timestamp >= :since GROUP BY category")
    Flux<Object[]> countByCategory(@Param("since") LocalDateTime since);

    @Query("SELECT action, COUNT(*) as count FROM audit_logs WHERE timestamp >= :since GROUP BY action")
    Flux<Object[]> countByAction(@Param("since") LocalDateTime since);

    @Query("SELECT service_name, COUNT(*) as count FROM audit_logs WHERE timestamp >= :since GROUP BY service_name")
    Flux<Object[]> countByService(@Param("since") LocalDateTime since);

    Mono<Long> countBySuccess(Boolean success);
}
