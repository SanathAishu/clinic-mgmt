package com.hospital.medicalrecords.event;

import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.MedicalRecordCreatedEvent;
import com.hospital.common.events.PrescriptionCreatedEvent;
import com.hospital.medicalrecords.entity.MedicalRecord;
import com.hospital.medicalrecords.entity.Prescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalRecordEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishMedicalRecordCreated(MedicalRecord record) {
        MedicalRecordCreatedEvent event = MedicalRecordCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("MedicalRecordCreated")
                .medicalRecordId(record.getId())
                .patientId(record.getPatientId())
                .doctorId(record.getDoctorId())
                .diagnosis(record.getDiagnosis())
                .recordDate(record.getRecordDate())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_TOPIC_EXCHANGE,
                RabbitMQConfig.MEDICAL_RECORD_CREATED_KEY,
                event
        );

        log.info("Published MedicalRecordCreatedEvent for record ID: {}", record.getId());
    }

    public void publishPrescriptionCreated(Prescription prescription) {
        PrescriptionCreatedEvent event = PrescriptionCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("PrescriptionCreated")
                .prescriptionId(prescription.getId())
                .medicalRecordId(prescription.getMedicalRecord().getId())
                .patientId(prescription.getPatientId())
                .doctorId(prescription.getDoctorId())
                .medications(prescription.getMedications())
                .prescriptionDate(prescription.getPrescriptionDate())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_TOPIC_EXCHANGE,
                RabbitMQConfig.PRESCRIPTION_CREATED_KEY,
                event
        );

        log.info("Published PrescriptionCreatedEvent for prescription ID: {}", prescription.getId());
    }
}
