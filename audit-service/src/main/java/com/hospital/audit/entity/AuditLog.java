package com.hospital.audit.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log entity for tracking all system events and changes.
 *
 * Multi-Tenancy:
 * - tenantId field ensures audit log isolation per tenant
 * - All queries MUST filter by tenantId
 *
 * DPDPA Compliance:
 * - Immutable audit trail (no updates/deletes allowed)
 * - Tracks all data access and modifications
 * - Retention: 7 years minimum for healthcare data
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_tenant_timestamp", columnList = "tenant_id, timestamp DESC"),
    @Index(name = "idx_audit_user_id", columnList = "user_id, timestamp DESC"),
    @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id, timestamp DESC"),
    @Index(name = "idx_audit_action", columnList = "action, timestamp DESC"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp DESC")
})
public class AuditLog extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant discriminator - CRITICAL for audit log isolation.
     */
    @Column(name = "tenant_id", nullable = false, length = 50, updatable = false)
    private String tenantId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "user_email", length = 255, updatable = false)
    private String userEmail;

    @Column(nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @Column(length = 500, updatable = false)
    private String description;

    @Column(name = "old_value", columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 500, updatable = false)
    private String userAgent;

    @Column(name = "event_id", updatable = false)
    private UUID eventId;

    @Column(name = "http_method", length = 10, updatable = false)
    private String httpMethod;

    @Column(name = "request_path", length = 500, updatable = false)
    private String requestPath;

    @Column(name = "status_code", updatable = false)
    private Integer statusCode;

    /**
     * Timestamp when the action occurred.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Constructors

    public AuditLog() {
    }

    public AuditLog(String tenantId, UUID userId, String action, String resourceType, UUID resourceId) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public AuditLog(String tenantId, UUID userId, String userEmail, String action,
                    String resourceType, UUID resourceId, String description,
                    String oldValue, String newValue, String ipAddress,
                    String userAgent, String httpMethod, String requestPath,
                    Integer statusCode) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.description = description;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.statusCode = statusCode;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
