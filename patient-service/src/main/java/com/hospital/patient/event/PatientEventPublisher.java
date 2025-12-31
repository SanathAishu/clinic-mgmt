package com.hospital.patient.event;

import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.PatientCreatedEvent;
import com.hospital.common.events.PatientDeletedEvent;
import com.hospital.common.events.PatientUpdatedEvent;
import com.hospital.patient.entity.Patient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publisher for patient domain events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish PatientCreatedEvent
     */
    public void publishPatientCreated(Patient patient) {
        PatientCreatedEvent event = PatientCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("PatientCreated")
                .patientId(patient.getId())
                .name(patient.getName())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .gender(patient.getGender())
                .disease(patient.getDisease())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_DIRECT_EXCHANGE,
                RabbitMQConfig.PATIENT_CREATED_KEY,
                event
        );

        log.info("Published PatientCreatedEvent for patient ID: {}", patient.getId());
    }

    /**
     * Publish PatientUpdatedEvent
     */
    public void publishPatientUpdated(Patient patient) {
        PatientUpdatedEvent event = PatientUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("PatientUpdated")
                .patientId(patient.getId())
                .name(patient.getName())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .gender(patient.getGender())
                .disease(patient.getDisease())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_DIRECT_EXCHANGE,
                RabbitMQConfig.PATIENT_UPDATED_KEY,
                event
        );

        log.info("Published PatientUpdatedEvent for patient ID: {}", patient.getId());
    }

    /**
     * Publish PatientDeletedEvent
     */
    public void publishPatientDeleted(UUID patientId) {
        PatientDeletedEvent event = PatientDeletedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("PatientDeleted")
                .patientId(patientId)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_DIRECT_EXCHANGE,
                RabbitMQConfig.PATIENT_DELETED_KEY,
                event
        );

        log.info("Published PatientDeletedEvent for patient ID: {}", patientId);
    }
}
