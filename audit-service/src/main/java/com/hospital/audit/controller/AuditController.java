package com.hospital.audit.controller;

import com.hospital.audit.dto.AuditLogDto;
import com.hospital.audit.service.AuditService;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Audit REST controller for querying audit logs.
 *
 * Security:
 * - All endpoints require authentication (@Authenticated)
 * - Most endpoints require ADMIN role (@RolesAllowed)
 * - Tenant isolation via JWT claims
 *
 * Endpoints:
 * - GET /api/audit/logs - Get all audit logs (paginated)
 * - GET /api/audit/logs/user/{userId} - Get user activity
 * - GET /api/audit/logs/{resourceType}/{resourceId} - Get resource history
 * - GET /api/audit/logs/recent - Get recent logs
 * - GET /api/audit/statistics - Get audit statistics
 */
@Path("/api/audit")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class AuditController {

    @Inject
    AuditService auditService;

    @Inject
    JsonWebToken jwt;

    /**
     * Get all audit logs for tenant (paginated).
     *
     * Query params:
     * - page: Page number (default 0)
     * - size: Page size (default 50, max 100)
     *
     * @return List of audit logs
     */
    @GET
    @Path("/logs")
    @RolesAllowed({"ADMIN", "AUDITOR"})
    public Uni<Response> getAuditLogs(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("50") int size
    ) {
        String tenantId = jwt.getClaim("tenantId");

        // Validate and limit page size
        if (size > 100) size = 100;

        Log.infof("Getting audit logs: tenantId=%s, page=%d, size=%d", tenantId, page, size);

        return auditService.getAuditLogs(tenantId, page, size)
            .map(logs -> Response.ok(logs).build());
    }

    /**
     * Get audit logs for a specific user.
     *
     * @param userId User ID
     * @param page Page number
     * @param size Page size
     * @return List of user's audit logs
     */
    @GET
    @Path("/logs/user/{userId}")
    @RolesAllowed({"ADMIN", "AUDITOR"})
    public Uni<Response> getUserAuditLogs(
        @PathParam("userId") UUID userId,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("50") int size
    ) {
        String tenantId = jwt.getClaim("tenantId");

        if (size > 100) size = 100;

        Log.infof("Getting user audit logs: tenantId=%s, userId=%s", tenantId, userId);

        return auditService.getAuditLogsByUser(tenantId, userId, page, size)
            .map(logs -> Response.ok(logs).build());
    }

    /**
     * Get audit history for a specific resource.
     *
     * @param resourceType Resource type (e.g., "PATIENT", "APPOINTMENT")
     * @param resourceId Resource ID
     * @return Stream of audit logs for the resource
     */
    @GET
    @Path("/logs/{resourceType}/{resourceId}")
    @RolesAllowed({"ADMIN", "AUDITOR"})
    public Uni<List<AuditLogDto>> getResourceHistory(
        @PathParam("resourceType") String resourceType,
        @PathParam("resourceId") UUID resourceId
    ) {
        String tenantId = jwt.getClaim("tenantId");

        Log.infof("Getting resource history: tenantId=%s, resourceType=%s, resourceId=%s",
            tenantId, resourceType, resourceId);

        return auditService.getResourceHistory(tenantId, resourceType.toUpperCase(), resourceId);
    }

    /**
     * Get audit logs by action type.
     *
     * @param action Action type (CREATE, UPDATE, DELETE, etc.)
     * @param page Page number
     * @param size Page size
     * @return List of audit logs
     */
    @GET
    @Path("/logs/action/{action}")
    @RolesAllowed({"ADMIN", "AUDITOR"})
    public Uni<Response> getAuditLogsByAction(
        @PathParam("action") String action,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("50") int size
    ) {
        String tenantId = jwt.getClaim("tenantId");

        if (size > 100) size = 100;

        Log.infof("Getting audit logs by action: tenantId=%s, action=%s", tenantId, action);

        return auditService.getAuditLogsByAction(tenantId, action.toUpperCase(), page, size)
            .map(logs -> Response.ok(logs).build());
    }

    /**
     * Get audit logs within a date range.
     *
     * Query params:
     * - startDate: Start date/time (ISO format)
     * - endDate: End date/time (ISO format)
     *
     * @param startDate Start date string
     * @param endDate End date string
     * @param page Page number
     * @param size Page size
     * @return List of audit logs
     */
    @GET
    @Path("/logs/daterange")
    @RolesAllowed({"ADMIN", "AUDITOR"})
    public Uni<Response> getAuditLogsByDateRange(
        @QueryParam("startDate") String startDate,
        @QueryParam("endDate") String endDate,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("50") int size
    ) {
        String tenantId = jwt.getClaim("tenantId");

        if (size > 100) size = 100;

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime start = LocalDateTime.parse(startDate, formatter);
            LocalDateTime end = LocalDateTime.parse(endDate, formatter);

            Log.infof("Getting audit logs by date range: tenantId=%s, start=%s, end=%s",
                tenantId, start, end);

            return auditService.getAuditLogsByDateRange(tenantId, start, end, page, size)
                .map(logs -> Response.ok(logs).build());
        } catch (Exception e) {
            Log.errorf(e, "Invalid date format");
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid date format. Use ISO format: yyyy-MM-dd'T'HH:mm:ss"))
                    .build()
            );
        }
    }

    /**
     * Get recent audit logs.
     *
     * @param limit Number of entries (default 20, max 100)
     * @return List of recent audit logs
     */
    @GET
    @Path("/logs/recent")
    @RolesAllowed({"ADMIN", "AUDITOR"})
    public Uni<Response> getRecentAuditLogs(
        @QueryParam("limit") @DefaultValue("20") int limit
    ) {
        String tenantId = jwt.getClaim("tenantId");

        if (limit > 100) limit = 100;

        Log.infof("Getting recent audit logs: tenantId=%s, limit=%d", tenantId, limit);

        return auditService.getRecentAuditLogs(tenantId, limit)
            .map(logs -> Response.ok(logs).build());
    }

    /**
     * Get failed operations (HTTP status >= 400).
     *
     * @param page Page number
     * @param size Page size
     * @return List of failed operations
     */
    @GET
    @Path("/logs/failed")
    @RolesAllowed({"ADMIN", "AUDITOR"})
    public Uni<Response> getFailedOperations(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("50") int size
    ) {
        String tenantId = jwt.getClaim("tenantId");

        if (size > 100) size = 100;

        Log.infof("Getting failed operations: tenantId=%s", tenantId);

        return auditService.getFailedOperations(tenantId, page, size)
            .map(logs -> Response.ok(logs).build());
    }

    /**
     * Search audit logs by description.
     *
     * @param q Search term
     * @param page Page number
     * @param size Page size
     * @return List of matching audit logs
     */
    @GET
    @Path("/logs/search")
    @RolesAllowed({"ADMIN", "AUDITOR"})
    public Uni<Response> searchAuditLogs(
        @QueryParam("q") String q,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("50") int size
    ) {
        String tenantId = jwt.getClaim("tenantId");

        if (q == null || q.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Search term (q) is required"))
                    .build()
            );
        }

        if (size > 100) size = 100;

        Log.infof("Searching audit logs: tenantId=%s, query=%s", tenantId, q);

        return auditService.searchAuditLogs(tenantId, q, page, size)
            .map(logs -> Response.ok(logs).build());
    }

    /**
     * Get audit statistics for tenant.
     *
     * @return Audit statistics
     */
    @GET
    @Path("/statistics")
    @RolesAllowed({"ADMIN", "AUDITOR"})
    public Uni<Response> getStatistics() {
        String tenantId = jwt.getClaim("tenantId");

        Log.infof("Getting audit statistics: tenantId=%s", tenantId);

        return auditService.getAuditStatistics(tenantId)
            .map(stats -> Response.ok(stats).build());
    }

    /**
     * Health check endpoint.
     *
     * @return OK status
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(new HealthResponse("UP", "audit-service")).build();
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class HealthResponse {
        public String status;
        public String service;

        public HealthResponse(String status, String service) {
            this.status = status;
            this.service = service;
        }
    }
}
