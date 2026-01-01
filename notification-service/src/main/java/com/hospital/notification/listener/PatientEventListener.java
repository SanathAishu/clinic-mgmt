package com.hospital.notification.listener;

import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.PatientCreatedEvent;
import com.hospital.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ event listener for patient-related events.
 * Handles patient creation notifications.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PatientEventListener {

    private final EmailService emailService;

    /**
     * Handles patient created events.
     * Sends welcome email to newly registered patients.
     *
     * @param event the patient created event
     */
    @RabbitListener(queues = RabbitMQConfig.PATIENT_UPDATES_QUEUE)
    public void handlePatientCreated(PatientCreatedEvent event) {
        log.info("Received PatientCreatedEvent: patientId={}, name={}, email={}",
                event.getPatientId(),
                event.getName(),
                event.getEmail());

        try {
            if (event.getEmail() != null && !event.getEmail().isEmpty()) {
                emailService.sendWelcomeEmail(
                        event.getEmail(),
                        event.getName()
                );
                log.info("Welcome email sent to new patient: {}", event.getEmail());
            } else {
                log.warn("Patient email is missing for patientId={}", event.getPatientId());
            }

            log.info("Successfully processed PatientCreatedEvent for patientId={}", event.getPatientId());

        } catch (Exception e) {
            log.error("Error processing PatientCreatedEvent for patientId={}: {}",
                    event.getPatientId(), e.getMessage(), e);
        }
    }
}
