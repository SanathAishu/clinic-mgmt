package com.hospital.audit.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.audit.entity.AuditLog;
import com.hospital.audit.repository.AuditLogRepository;
import com.hospital.common.event.AuthEvents.UserRegisteredEvent;
import com.hospital.common.event.AuthEvents.UserUpdatedEvent;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * SmallRye Reactive Messaging listener for user events.
 *
 * Using Quarkus 3.15.1 to avoid context propagation bug in 3.17.0.
 */
@ApplicationScoped
public class UserEventListener {

    @Inject
    AuditLogRepository auditLogRepository;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("user-registered-events")
    @WithTransaction
    public Uni<Void> handleUserRegistered(JsonObject jsonPayload) {
        // Convert JsonObject to UserRegisteredEvent
        UserRegisteredEvent event;
        try {
            event = objectMapper.readValue(jsonPayload.encode(), UserRegisteredEvent.class);
        } catch (Exception e) {
            Log.errorf(e, "Failed to deserialize UserRegisteredEvent: %s", jsonPayload);
            return Uni.createFrom().voidItem();
        }
        if (event.getTenantId() == null || event.getTenantId().isBlank()) {
            Log.errorf("UserRegisteredEvent missing tenantId: eventId=%s", event.getEventId());
            return Uni.createFrom().voidItem();
        }

        Log.infof("Processing UserRegisteredEvent: tenantId=%s, userId=%s, email=%s",
            event.getTenantId(), event.getUserId(), event.getEmail());

        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(event.getTenantId());
        auditLog.setUserId(event.getUserId());
        auditLog.setUserEmail(event.getEmail());
        auditLog.setAction("REGISTER");
        auditLog.setResourceType("USER");
        auditLog.setResourceId(event.getUserId());
        auditLog.setDescription(String.format("User registered: %s (%s)", event.getName(), event.getEmail()));
        auditLog.setEventId(event.getEventId());

        String newValue = String.format(
            "{\"name\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}",
            event.getName(), event.getEmail(), event.getRole()
        );
        auditLog.setNewValue(newValue);

        return auditLogRepository.persist(auditLog)
            .replaceWithVoid()
            .onFailure().invoke(error ->
                Log.errorf(error, "Failed to create audit log for UserRegisteredEvent: %s", event.getEventId())
            );
    }

    @Incoming("user-updated-events")
    @WithTransaction
    public Uni<Void> handleUserUpdated(JsonObject jsonPayload) {
        // Convert JsonObject to UserUpdatedEvent
        UserUpdatedEvent event;
        try {
            event = objectMapper.readValue(jsonPayload.encode(), UserUpdatedEvent.class);
        } catch (Exception e) {
            Log.errorf(e, "Failed to deserialize UserUpdatedEvent: %s", jsonPayload);
            return Uni.createFrom().voidItem();
        }
        if (event.getTenantId() == null || event.getTenantId().isBlank()) {
            Log.errorf("UserUpdatedEvent missing tenantId: eventId=%s", event.getEventId());
            return Uni.createFrom().voidItem();
        }

        Log.infof("Processing UserUpdatedEvent: tenantId=%s, userId=%s",
            event.getTenantId(), event.getUserId());

        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(event.getTenantId());
        auditLog.setUserId(event.getUserId());
        auditLog.setAction("UPDATE");
        auditLog.setResourceType("USER");
        auditLog.setResourceId(event.getUserId());
        auditLog.setDescription("User profile updated");
        auditLog.setEventId(event.getEventId());

        if (event.getChanges() != null && !event.getChanges().isEmpty()) {
            try {
                StringBuilder changesJson = new StringBuilder("{");
                event.getChanges().forEach((key, value) -> {
                    changesJson.append("\"").append(key).append("\":\"")
                               .append(value).append("\",");
                });
                if (changesJson.length() > 1) {
                    changesJson.setLength(changesJson.length() - 1);
                }
                changesJson.append("}");
                auditLog.setNewValue(changesJson.toString());
            } catch (Exception e) {
                Log.warnf(e, "Failed to serialize changes for UserUpdatedEvent: %s", event.getEventId());
            }
        }

        return auditLogRepository.persist(auditLog)
            .replaceWithVoid()
            .onFailure().invoke(error ->
                Log.errorf(error, "Failed to create audit log for UserUpdatedEvent: %s", event.getEventId())
            );
    }
}
