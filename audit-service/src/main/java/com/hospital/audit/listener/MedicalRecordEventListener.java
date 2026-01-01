package com.hospital.audit.listener;

import com.hospital.audit.dto.CreateAuditLogRequest;
import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.service.AuditLogService;
import com.hospital.common.events.MedicalRecordCreatedEvent;
import com.hospital.common.events.PrescriptionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalRecordEventListener {

    private final AuditLogService auditLogService;

    @RabbitListener(queues = "#{@medicalRecordAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handleMedicalRecordCreated(MedicalRecordCreatedEvent event) {
        log.info("Received medical record created event: {}", event.getMedicalRecordId());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.MEDICAL_RECORD)
                .action(AuditAction.CREATE)
                .serviceName("medical-records-service")
                .entityType("MedicalRecord")
                .entityId(event.getMedicalRecordId().toString())
                .description(String.format("Medical record created for patient %s: %s",
                    event.getPatientId(), event.getDiagnosis()))
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }

    @RabbitListener(queues = "#{@prescriptionAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handlePrescriptionCreated(PrescriptionCreatedEvent event) {
        log.info("Received prescription created event: {}", event.getPrescriptionId());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.PRESCRIPTION)
                .action(AuditAction.CREATE)
                .serviceName("medical-records-service")
                .entityType("Prescription")
                .entityId(event.getPrescriptionId().toString())
                .description(String.format("Prescription created for patient %s: %s",
                    event.getPatientId(), event.getMedications()))
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }
}
