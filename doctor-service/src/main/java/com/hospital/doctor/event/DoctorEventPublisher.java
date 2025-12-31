package com.hospital.doctor.event;

import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.DoctorCreatedEvent;
import com.hospital.common.events.DoctorUpdatedEvent;
import com.hospital.doctor.entity.Doctor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publisher for doctor domain events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish DoctorCreatedEvent
     */
    public void publishDoctorCreated(Doctor doctor) {
        DoctorCreatedEvent event = DoctorCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("DoctorCreated")
                .doctorId(doctor.getId())
                .name(doctor.getName())
                .email(doctor.getEmail())
                .phone(doctor.getPhone())
                .gender(doctor.getGender())
                .specialty(doctor.getSpecialty())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_DIRECT_EXCHANGE,
                RabbitMQConfig.DOCTOR_CREATED_KEY,
                event
        );

        log.info("Published DoctorCreatedEvent for doctor ID: {}", doctor.getId());
    }

    /**
     * Publish DoctorUpdatedEvent
     */
    public void publishDoctorUpdated(Doctor doctor) {
        DoctorUpdatedEvent event = DoctorUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("DoctorUpdated")
                .doctorId(doctor.getId())
                .name(doctor.getName())
                .email(doctor.getEmail())
                .phone(doctor.getPhone())
                .gender(doctor.getGender())
                .specialty(doctor.getSpecialty())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_DIRECT_EXCHANGE,
                RabbitMQConfig.DOCTOR_UPDATED_KEY,
                event
        );

        log.info("Published DoctorUpdatedEvent for doctor ID: {}", doctor.getId());
    }
}
