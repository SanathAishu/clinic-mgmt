package com.hospital.audit.listener;

import com.hospital.audit.dto.CreateAuditLogRequest;
import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.service.AuditLogService;
import com.hospital.common.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    private final AuditLogService auditLogService;

    @RabbitListener(queues = "#{@authAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user registered event for user: {}", event.getEmail());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.AUTHENTICATION)
                .action(AuditAction.REGISTER)
                .serviceName("auth-service")
                .entityType("User")
                .entityId(event.getUserId() != null ? event.getUserId().toString() : null)
                .userEmail(event.getEmail())
                .userRole(event.getRole())
                .description("User registered: " + event.getEmail() + " with role " + event.getRole())
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }
}
