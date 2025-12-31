package com.hospital.appointment.event;

import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.DoctorSnapshot;
import com.hospital.appointment.entity.PatientSnapshot;
import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.AppointmentCancelledEvent;
import com.hospital.common.events.AppointmentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publisher for appointment domain events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish AppointmentCreatedEvent
     */
    public void publishAppointmentCreated(Appointment appointment, PatientSnapshot patient, DoctorSnapshot doctor) {
        AppointmentCreatedEvent event = AppointmentCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("AppointmentCreated")
                .appointmentId(appointment.getId())
                .patientId(appointment.getPatientId())
                .patientName(patient.getName())
                .patientEmail(patient.getEmail())
                .doctorId(appointment.getDoctorId())
                .doctorName(doctor.getName())
                .doctorEmail(doctor.getEmail())
                .appointmentDate(appointment.getAppointmentDate())
                .status(appointment.getStatus())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_TOPIC_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_CREATED_KEY,
                event
        );

        log.info("Published AppointmentCreatedEvent for appointment ID: {}", appointment.getId());
    }

    /**
     * Publish AppointmentCancelledEvent
     */
    public void publishAppointmentCancelled(Appointment appointment) {
        AppointmentCancelledEvent event = AppointmentCancelledEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("AppointmentCancelled")
                .appointmentId(appointment.getId())
                .patientId(appointment.getPatientId())
                .doctorId(appointment.getDoctorId())
                .appointmentDate(appointment.getAppointmentDate())
                .cancellationReason(appointment.getNotes())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_TOPIC_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_CANCELLED_KEY,
                event
        );

        log.info("Published AppointmentCancelledEvent for appointment ID: {}", appointment.getId());
    }
}
