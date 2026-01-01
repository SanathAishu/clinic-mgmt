package com.hospital.appointment.consumer;

import com.hospital.appointment.entity.DoctorSnapshot;
import com.hospital.appointment.repository.DoctorSnapshotRepository;
import com.hospital.appointment.service.CacheEvictionService;
import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.DoctorCreatedEvent;
import com.hospital.common.events.DoctorUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes doctor events to update doctor snapshots and invalidate caches.
 * Uses class-level @RabbitListener with @RabbitHandler for type-based message routing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitMQConfig.DOCTOR_UPDATES_QUEUE)
public class DoctorEventConsumer {

    private final DoctorSnapshotRepository doctorSnapshotRepository;
    private final CacheEvictionService cacheEvictionService;

    @RabbitHandler
    public void handleDoctorCreatedEvent(DoctorCreatedEvent event) {
        log.info("Received DoctorCreatedEvent for doctor ID: {}", event.getDoctorId());

        DoctorSnapshot snapshot = DoctorSnapshot.builder()
                .doctorId(event.getDoctorId())
                .name(event.getName())
                .email(event.getEmail())
                .phone(event.getPhone())
                .gender(event.getGender())
                .specialty(event.getSpecialty())
                .build();

        doctorSnapshotRepository.save(snapshot);

        // Evict related caches
        cacheEvictionService.evictAppointmentsCacheForDoctor(event.getDoctorId());

        log.info("Doctor snapshot created for doctor ID: {}", event.getDoctorId());
    }

    @RabbitHandler
    public void handleDoctorUpdatedEvent(DoctorUpdatedEvent event) {
        log.info("Received DoctorUpdatedEvent for doctor ID: {}", event.getDoctorId());

        doctorSnapshotRepository.findById(event.getDoctorId())
                .ifPresentOrElse(
                        snapshot -> {
                            snapshot.setName(event.getName());
                            snapshot.setEmail(event.getEmail());
                            snapshot.setPhone(event.getPhone());
                            snapshot.setGender(event.getGender());
                            snapshot.setSpecialty(event.getSpecialty());
                            doctorSnapshotRepository.save(snapshot);
                            log.info("Doctor snapshot updated for doctor ID: {}", event.getDoctorId());
                        },
                        () -> {
                            // Create snapshot if it doesn't exist
                            DoctorSnapshot newSnapshot = DoctorSnapshot.builder()
                                    .doctorId(event.getDoctorId())
                                    .name(event.getName())
                                    .email(event.getEmail())
                                    .phone(event.getPhone())
                                    .gender(event.getGender())
                                    .specialty(event.getSpecialty())
                                    .build();
                            doctorSnapshotRepository.save(newSnapshot);
                            log.warn("Doctor snapshot didn't exist, created new snapshot for doctor ID: {}", event.getDoctorId());
                        }
                );

        // Evict related caches
        cacheEvictionService.evictAppointmentsCacheForDoctor(event.getDoctorId());
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknownEvent(Object event) {
        log.warn("Received unknown event type on doctor.updates queue: {}", event.getClass().getName());
    }
}
