package com.hospital.appointment.consumer;

import com.hospital.appointment.entity.PatientSnapshot;
import com.hospital.appointment.repository.PatientSnapshotRepository;
import com.hospital.appointment.service.CacheEvictionService;
import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.PatientCreatedEvent;
import com.hospital.common.events.PatientDeletedEvent;
import com.hospital.common.events.PatientUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes patient events to update patient snapshots and invalidate caches
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientEventConsumer {

    private final PatientSnapshotRepository patientSnapshotRepository;
    private final CacheEvictionService cacheEvictionService;

    @RabbitListener(queues = RabbitMQConfig.PATIENT_UPDATES_QUEUE)
    public void handlePatientCreatedEvent(PatientCreatedEvent event) {
        log.info("Received PatientCreatedEvent for patient ID: {}", event.getPatientId());

        PatientSnapshot snapshot = PatientSnapshot.builder()
                .patientId(event.getPatientId())
                .name(event.getName())
                .email(event.getEmail())
                .phone(event.getPhone())
                .gender(event.getGender())
                .disease(event.getDisease())
                .build();

        patientSnapshotRepository.save(snapshot);

        // Evict related caches
        cacheEvictionService.evictAppointmentsCacheForPatient(event.getPatientId());

        log.info("Patient snapshot created for patient ID: {}", event.getPatientId());
    }

    @RabbitListener(queues = RabbitMQConfig.PATIENT_UPDATES_QUEUE)
    public void handlePatientUpdatedEvent(PatientUpdatedEvent event) {
        log.info("Received PatientUpdatedEvent for patient ID: {}", event.getPatientId());

        patientSnapshotRepository.findById(event.getPatientId())
                .ifPresentOrElse(
                        snapshot -> {
                            snapshot.setName(event.getName());
                            snapshot.setEmail(event.getEmail());
                            snapshot.setPhone(event.getPhone());
                            snapshot.setGender(event.getGender());
                            snapshot.setDisease(event.getDisease());
                            patientSnapshotRepository.save(snapshot);
                            log.info("Patient snapshot updated for patient ID: {}", event.getPatientId());
                        },
                        () -> {
                            // Create snapshot if it doesn't exist
                            PatientSnapshot newSnapshot = PatientSnapshot.builder()
                                    .patientId(event.getPatientId())
                                    .name(event.getName())
                                    .email(event.getEmail())
                                    .phone(event.getPhone())
                                    .gender(event.getGender())
                                    .disease(event.getDisease())
                                    .build();
                            patientSnapshotRepository.save(newSnapshot);
                            log.warn("Patient snapshot didn't exist, created new snapshot for patient ID: {}", event.getPatientId());
                        }
                );

        // Evict related caches
        cacheEvictionService.evictAppointmentsCacheForPatient(event.getPatientId());
    }

    @RabbitListener(queues = RabbitMQConfig.PATIENT_UPDATES_QUEUE)
    public void handlePatientDeletedEvent(PatientDeletedEvent event) {
        log.info("Received PatientDeletedEvent for patient ID: {}", event.getPatientId());

        patientSnapshotRepository.deleteById(event.getPatientId());

        // Evict related caches
        cacheEvictionService.evictAppointmentsCacheForPatient(event.getPatientId());

        log.info("Patient snapshot deleted for patient ID: {}", event.getPatientId());
    }
}
