package com.hospital.notification.listener;

import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.AppointmentCreatedEvent;
import com.hospital.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ event listener for appointment-related events.
 * Handles appointment creation and reminder notifications.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final EmailService emailService;

    /**
     * Handles appointment created events.
     * Sends confirmation emails to both patient and doctor.
     *
     * @param event the appointment created event
     */
    @RabbitListener(queues = RabbitMQConfig.APPOINTMENT_NOTIFICATIONS_QUEUE)
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Received AppointmentCreatedEvent: appointmentId={}, patientId={}, doctorId={}, appointmentDate={}",
                event.getAppointmentId(),
                event.getPatientId(),
                event.getDoctorId(),
                event.getAppointmentDate());

        try {
            // Send confirmation email to patient
            if (event.getPatientEmail() != null && !event.getPatientEmail().isEmpty()) {
                emailService.sendAppointmentConfirmation(
                        event.getPatientEmail(),
                        event.getPatientName(),
                        event.getDoctorName(),
                        event.getAppointmentDate()
                );
                log.info("Appointment confirmation email sent to patient: {}", event.getPatientEmail());
            } else {
                log.warn("Patient email is missing for appointmentId={}", event.getAppointmentId());
            }

            // Send notification email to doctor
            if (event.getDoctorEmail() != null && !event.getDoctorEmail().isEmpty()) {
                emailService.sendAppointmentConfirmation(
                        event.getDoctorEmail(),
                        event.getPatientName(),
                        event.getDoctorName(),
                        event.getAppointmentDate()
                );
                log.info("Appointment notification email sent to doctor: {}", event.getDoctorEmail());
            } else {
                log.warn("Doctor email is missing for appointmentId={}", event.getAppointmentId());
            }

            log.info("Successfully processed AppointmentCreatedEvent for appointmentId={}", event.getAppointmentId());

        } catch (Exception e) {
            log.error("Error processing AppointmentCreatedEvent for appointmentId={}: {}",
                    event.getAppointmentId(), e.getMessage(), e);
        }
    }

    /**
     * Handles appointment reminder events.
     * Sends reminder emails to patients before their scheduled appointments.
     *
     * @param event the appointment reminder event (using AppointmentCreatedEvent structure)
     */
    public void handleAppointmentReminder(AppointmentCreatedEvent event) {
        log.info("Received AppointmentReminderEvent: appointmentId={}, patientId={}, appointmentDate={}",
                event.getAppointmentId(),
                event.getPatientId(),
                event.getAppointmentDate());

        try {
            if (event.getPatientEmail() != null && !event.getPatientEmail().isEmpty()) {
                emailService.sendAppointmentReminder(
                        event.getPatientEmail(),
                        event.getPatientName(),
                        event.getDoctorName(),
                        event.getAppointmentDate()
                );
                log.info("Appointment reminder email sent to patient: {}", event.getPatientEmail());
            } else {
                log.warn("Patient email is missing for appointment reminder, appointmentId={}", event.getAppointmentId());
            }

            log.info("Successfully processed AppointmentReminderEvent for appointmentId={}", event.getAppointmentId());

        } catch (Exception e) {
            log.error("Error processing AppointmentReminderEvent for appointmentId={}: {}",
                    event.getAppointmentId(), e.getMessage(), e);
        }
    }
}
