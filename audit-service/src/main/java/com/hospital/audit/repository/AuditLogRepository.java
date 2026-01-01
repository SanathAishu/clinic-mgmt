package com.hospital.audit.repository;

import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByCategory(AuditCategory category, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findByEntityId(String entityId, Pageable pageable);

    Page<AuditLog> findByServiceName(String serviceName, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE a.category = :category AND a.action = :action ORDER BY a.timestamp DESC")
    Page<AuditLog> findByCategoryAndAction(
        @Param("category") AuditCategory category,
        @Param("action") AuditAction action,
        Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<AuditLog> findEntityHistory(
        @Param("entityType") String entityType,
        @Param("entityId") String entityId
    );

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findUserActivityInRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT a.category, COUNT(a) FROM AuditLog a WHERE a.timestamp >= :since GROUP BY a.category")
    List<Object[]> countByCategory(@Param("since") LocalDateTime since);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.timestamp >= :since GROUP BY a.action")
    List<Object[]> countByAction(@Param("since") LocalDateTime since);

    @Query("SELECT a.serviceName, COUNT(a) FROM AuditLog a WHERE a.timestamp >= :since GROUP BY a.serviceName")
    List<Object[]> countByService(@Param("since") LocalDateTime since);

    long countBySuccess(Boolean success);
}
