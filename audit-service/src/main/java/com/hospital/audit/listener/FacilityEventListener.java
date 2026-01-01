package com.hospital.audit.listener;

import com.hospital.audit.dto.CreateAuditLogRequest;
import com.hospital.audit.entity.AuditAction;
import com.hospital.audit.entity.AuditCategory;
import com.hospital.audit.service.AuditLogService;
import com.hospital.common.events.PatientAdmittedEvent;
import com.hospital.common.events.PatientDischargedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FacilityEventListener {

    private final AuditLogService auditLogService;

    @RabbitListener(queues = "#{@facilityAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handlePatientAdmitted(PatientAdmittedEvent event) {
        log.info("Received patient admitted event: patient {} to room {}",
            event.getPatientId(), event.getRoomNumber());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.FACILITY)
                .action(AuditAction.ADMIT)
                .serviceName("facility-service")
                .entityType("RoomBooking")
                .entityId(event.getBookingId() != null ? event.getBookingId().toString() : null)
                .description(String.format("Patient %s admitted to room %s",
                    event.getPatientId(), event.getRoomNumber()))
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }

    @RabbitListener(queues = "#{@dischargeAuditQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void handlePatientDischarged(PatientDischargedEvent event) {
        log.info("Received patient discharged event: patient {} from room {}",
            event.getPatientId(), event.getRoomNumber());

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .category(AuditCategory.FACILITY)
                .action(AuditAction.DISCHARGE)
                .serviceName("facility-service")
                .entityType("RoomBooking")
                .entityId(event.getBookingId() != null ? event.getBookingId().toString() : null)
                .description(String.format("Patient %s discharged from room %s",
                    event.getPatientId(), event.getRoomNumber()))
                .success(true)
                .build();

        auditLogService.createAuditLog(request);
    }
}
