package com.hospital.medicalrecords.service;

import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.medicalrecords.dto.*;
import com.hospital.medicalrecords.entity.MedicalRecord;
import com.hospital.medicalrecords.entity.Prescription;
import com.hospital.medicalrecords.repository.MedicalRecordRepository;
import com.hospital.medicalrecords.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ModelMapper modelMapper;
    private final WebClient webClient;

    @Transactional
    public Mono<PrescriptionDto> createPrescription(CreatePrescriptionRequest request) {
        log.info("Creating prescription for medical record: {}", request.getMedicalRecordId());

        return medicalRecordRepository.findById(request.getMedicalRecordId())
                .switchIfEmpty(Mono.error(new NotFoundException("Medical record", request.getMedicalRecordId())))
                .flatMap(medicalRecord -> {
                    if (medicalRecord.getPrescription() != null) {
                        return Mono.error(new ValidationException("Medical record already has a prescription"));
                    }

                    Prescription prescription = Prescription.builder()
                            .medicalRecord(medicalRecord)
                            .patientId(medicalRecord.getPatientId())
                            .doctorId(medicalRecord.getDoctorId())
                            .prescriptionDate(request.getPrescriptionDate())
                            .medications(request.getMedications())
                            .dosageInstructions(request.getDosageInstructions())
                            .durationDays(request.getDurationDays())
                            .specialInstructions(request.getSpecialInstructions())
                            .refillable(request.getRefillable() != null ? request.getRefillable() : false)
                            .refillsRemaining(request.getRefillsRemaining() != null ? request.getRefillsRemaining() : 0)
                            .active(true)
                            .build();

                    return prescriptionRepository.save(prescription);
                })
                .map(this::mapToDto)
                .doOnSuccess(dto -> {
                    log.info("Created prescription with ID: {}", dto.getId());
                    notifyPrescriptionCreated(dto).subscribe();
                });
    }

    @Transactional(readOnly = true)
    public Mono<PrescriptionDto> getPrescriptionById(UUID id) {
        log.info("Fetching prescription by ID: {}", id);

        return prescriptionRepository.findById(id)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Prescription", id)));
    }

    @Transactional(readOnly = true)
    public Flux<PrescriptionDto> getPrescriptionsByPatientId(UUID patientId) {
        log.info("Fetching prescriptions for patient: {}", patientId);
        return prescriptionRepository.findByPatientIdAndActiveTrue(patientId)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<PrescriptionDto> getPrescriptionsByDoctorId(UUID doctorId) {
        log.info("Fetching prescriptions for doctor: {}", doctorId);
        return prescriptionRepository.findByDoctorIdAndActiveTrue(doctorId)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<PrescriptionDto> getPrescriptionsByPatientIdAndDateRange(
            UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching prescriptions for patient {} between {} and {}", patientId, startDate, endDate);
        return prescriptionRepository.findByPatientIdAndDateRange(patientId, startDate, endDate)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<PrescriptionDto> getRefillablePrescriptions(UUID patientId) {
        log.info("Fetching refillable prescriptions for patient: {}", patientId);
        return prescriptionRepository.findRefillablePrescriptions(patientId)
                .map(this::mapToDto);
    }

    @Transactional
    public Mono<PrescriptionDto> refillPrescription(UUID id) {
        log.info("Refilling prescription: {}", id);

        return prescriptionRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Prescription", id)))
                .flatMap(prescription -> {
                    if (!prescription.getRefillable()) {
                        return Mono.error(new ValidationException("Prescription is not refillable"));
                    }

                    if (prescription.getRefillsRemaining() <= 0) {
                        return Mono.error(new ValidationException("No refills remaining for this prescription"));
                    }

                    prescription.setRefillsRemaining(prescription.getRefillsRemaining() - 1);
                    prescription.setPrescriptionDate(LocalDate.now());
                    return prescriptionRepository.save(prescription);
                })
                .map(this::mapToDto)
                .doOnSuccess(dto -> log.info("Refilled prescription: {}, refills remaining: {}",
                        dto.getId(), dto.getRefillsRemaining()));
    }

    @Transactional
    public Mono<Void> deletePrescription(UUID id) {
        log.info("Soft deleting prescription: {}", id);

        return prescriptionRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Prescription", id)))
                .flatMap(prescription -> {
                    prescription.setActive(false);
                    return prescriptionRepository.save(prescription);
                })
                .doOnSuccess(prescription -> log.info("Soft deleted prescription: {}", id))
                .then();
    }

    private PrescriptionDto mapToDto(Prescription prescription) {
        PrescriptionDto dto = modelMapper.map(prescription, PrescriptionDto.class);
        if (prescription.getMedicalRecordId() != null) {
            dto.setMedicalRecordId(prescription.getMedicalRecordId());
        } else if (prescription.getMedicalRecord() != null) {
            dto.setMedicalRecordId(prescription.getMedicalRecord().getId());
        }
        return dto;
    }

    private Mono<Void> notifyPrescriptionCreated(PrescriptionDto prescription) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/prescription-created")
                .bodyValue(prescription)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify prescription creation: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
