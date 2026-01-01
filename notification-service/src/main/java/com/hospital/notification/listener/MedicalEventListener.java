package com.hospital.notification.listener;

import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.MedicalRecordCreatedEvent;
import com.hospital.common.events.PrescriptionCreatedEvent;
import com.hospital.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ event listener for medical-related events.
 * Handles medical record and prescription creation notifications.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MedicalEventListener {

    private final EmailService emailService;

    /**
     * Handles medical record created events.
     * Logs the event for audit purposes. Email notification requires patient details
     * which may need to be fetched from patient service.
     *
     * @param event the medical record created event
     */
    @RabbitListener(queues = RabbitMQConfig.PRESCRIPTION_NOTIFICATIONS_QUEUE)
    public void handleMedicalRecordCreated(MedicalRecordCreatedEvent event) {
        log.info("Received MedicalRecordCreatedEvent: medicalRecordId={}, patientId={}, doctorId={}, diagnosis={}, recordDate={}",
                event.getMedicalRecordId(),
                event.getPatientId(),
                event.getDoctorId(),
                event.getDiagnosis(),
                event.getRecordDate());

        try {
            // Note: Email notification for medical records would require patient email
            // which is not included in the event. This could be fetched from patient service
            // or the event could be enriched to include patient contact information.
            log.info("Medical record created notification logged for patientId={}, medicalRecordId={}",
                    event.getPatientId(), event.getMedicalRecordId());

            log.info("Successfully processed MedicalRecordCreatedEvent for medicalRecordId={}",
                    event.getMedicalRecordId());

        } catch (Exception e) {
            log.error("Error processing MedicalRecordCreatedEvent for medicalRecordId={}: {}",
                    event.getMedicalRecordId(), e.getMessage(), e);
        }
    }

    /**
     * Handles prescription created events.
     * Logs the event and would send prescription notification if patient email is available.
     *
     * @param event the prescription created event
     */
    @RabbitListener(queues = RabbitMQConfig.PRESCRIPTION_NOTIFICATIONS_QUEUE)
    public void handlePrescriptionCreated(PrescriptionCreatedEvent event) {
        log.info("Received PrescriptionCreatedEvent: prescriptionId={}, patientId={}, doctorId={}, medications={}, prescriptionDate={}",
                event.getPrescriptionId(),
                event.getPatientId(),
                event.getDoctorId(),
                event.getMedications(),
                event.getPrescriptionDate());

        try {
            // Note: Email notification for prescriptions would require patient email
            // which is not included in the event. This could be fetched from patient service
            // or the event could be enriched to include patient contact information.
            // For now, we log the event for audit purposes.
            log.info("Prescription created notification logged for patientId={}, prescriptionId={}",
                    event.getPatientId(), event.getPrescriptionId());

            log.info("Successfully processed PrescriptionCreatedEvent for prescriptionId={}",
                    event.getPrescriptionId());

        } catch (Exception e) {
            log.error("Error processing PrescriptionCreatedEvent for prescriptionId={}: {}",
                    event.getPrescriptionId(), e.getMessage(), e);
        }
    }
}
