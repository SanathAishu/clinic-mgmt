package com.hospital.notification.listener;

import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.PatientAdmittedEvent;
import com.hospital.common.events.PatientDischargedEvent;
import com.hospital.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ event listener for facility-related events.
 * Handles patient admission and discharge notifications.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FacilityEventListener {

    private final EmailService emailService;

    /**
     * Handles patient admitted events.
     * Logs the admission event and would send notification if patient email is available.
     *
     * @param event the patient admitted event
     */
    @RabbitListener(queues = RabbitMQConfig.FACILITY_NOTIFICATIONS_QUEUE)
    public void handlePatientAdmitted(PatientAdmittedEvent event) {
        log.info("Received PatientAdmittedEvent: bookingId={}, patientId={}, roomId={}, roomNumber={}, admissionDate={}",
                event.getBookingId(),
                event.getPatientId(),
                event.getRoomId(),
                event.getRoomNumber(),
                event.getAdmissionDate());

        try {
            // Note: Email notification for admission would require patient email and name
            // which are not included in the event. This could be fetched from patient service
            // or the event could be enriched to include patient contact information.
            // Example if patient details were available:
            // emailService.sendAdmissionNotification(patientEmail, patientName, event.getRoomNumber(), event.getAdmissionDate());

            log.info("Patient admission notification logged for patientId={}, roomNumber={}, admissionDate={}",
                    event.getPatientId(), event.getRoomNumber(), event.getAdmissionDate());

            log.info("Successfully processed PatientAdmittedEvent for bookingId={}", event.getBookingId());

        } catch (Exception e) {
            log.error("Error processing PatientAdmittedEvent for bookingId={}: {}",
                    event.getBookingId(), e.getMessage(), e);
        }
    }

    /**
     * Handles patient discharged events.
     * Logs the discharge event and would send notification if patient email is available.
     *
     * @param event the patient discharged event
     */
    @RabbitListener(queues = RabbitMQConfig.FACILITY_NOTIFICATIONS_QUEUE)
    public void handlePatientDischarged(PatientDischargedEvent event) {
        log.info("Received PatientDischargedEvent: bookingId={}, patientId={}, roomId={}, roomNumber={}, dischargeDate={}",
                event.getBookingId(),
                event.getPatientId(),
                event.getRoomId(),
                event.getRoomNumber(),
                event.getDischargeDate());

        try {
            // Note: Email notification for discharge would require patient email and name
            // which are not included in the event. This could be fetched from patient service
            // or the event could be enriched to include patient contact information.
            // Example if patient details were available:
            // emailService.sendDischargeNotification(patientEmail, patientName, event.getRoomNumber(), event.getDischargeDate());

            log.info("Patient discharge notification logged for patientId={}, roomNumber={}, dischargeDate={}",
                    event.getPatientId(), event.getRoomNumber(), event.getDischargeDate());

            log.info("Successfully processed PatientDischargedEvent for bookingId={}", event.getBookingId());

        } catch (Exception e) {
            log.error("Error processing PatientDischargedEvent for bookingId={}: {}",
                    event.getBookingId(), e.getMessage(), e);
        }
    }
}
