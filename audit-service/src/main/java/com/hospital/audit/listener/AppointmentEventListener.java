package com.hospital.audit.listener;

import com.hospital.audit.dto.CreateAuditLogRequest;
import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.service.AuditLogService;
import com.hospital.common.events.AppointmentCreatedEvent;
import com.hospital.common.events.AppointmentCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final AuditLogService auditLogService;

    @RabbitListener(queues = "#{@appointmentAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Received appointment created event: {}", event.getAppointmentId());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.APPOINTMENT)
                .action(AuditAction.BOOK)
                .serviceName("appointment-service")
                .entityType("Appointment")
                .entityId(event.getAppointmentId().toString())
                .description(String.format("Appointment created for patient %s with doctor %s on %s",
                    event.getPatientId(), event.getDoctorId(), event.getAppointmentDate()))
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }

    @RabbitListener(queues = "#{@appointmentCancelAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Received appointment cancelled event: {}", event.getAppointmentId());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.APPOINTMENT)
                .action(AuditAction.CANCEL)
                .serviceName("appointment-service")
                .entityType("Appointment")
                .entityId(event.getAppointmentId().toString())
                .description("Appointment cancelled: " + event.getCancellationReason())
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }
}
