package com.hospital.audit.listener;

import com.hospital.audit.dto.CreateAuditLogRequest;
import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.service.AuditLogService;
import com.hospital.common.events.PatientCreatedEvent;
import com.hospital.common.events.PatientUpdatedEvent;
import com.hospital.common.events.PatientDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientEventListener {

    private final AuditLogService auditLogService;

    @RabbitListener(queues = "#{@patientAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handlePatientCreated(PatientCreatedEvent event) {
        log.info("Received patient created event for: {}", event.getPatientId());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.PATIENT)
                .action(AuditAction.CREATE)
                .serviceName("patient-service")
                .entityType("Patient")
                .entityId(event.getPatientId().toString())
                .userEmail(event.getEmail())
                .description("Patient created: " + event.getName())
                .newValue(buildPatientDetails(event))
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }

    @RabbitListener(queues = "#{@patientUpdateAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handlePatientUpdated(PatientUpdatedEvent event) {
        log.info("Received patient updated event for: {}", event.getPatientId());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.PATIENT)
                .action(AuditAction.UPDATE)
                .serviceName("patient-service")
                .entityType("Patient")
                .entityId(event.getPatientId().toString())
                .userEmail(event.getEmail())
                .description("Patient updated: " + event.getName())
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }

    @RabbitListener(queues = "#{@patientDeleteAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handlePatientDeleted(PatientDeletedEvent event) {
        log.info("Received patient deleted event for: {}", event.getPatientId());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.PATIENT)
                .action(AuditAction.DELETE)
                .serviceName("patient-service")
                .entityType("Patient")
                .entityId(event.getPatientId().toString())
                .description("Patient deleted")
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }

    private String buildPatientDetails(PatientCreatedEvent event) {
        return String.format("Name: %s, Email: %s, Disease: %s",
            event.getName(), event.getEmail(), event.getDisease());
    }
}
