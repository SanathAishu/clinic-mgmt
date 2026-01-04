package com.hospital.medicalrecords.service;

import com.hospital.common.exception.NotFoundException;
import com.hospital.medicalrecords.dto.*;
import com.hospital.medicalrecords.entity.MedicalRecord;
import com.hospital.medicalrecords.repository.MedicalRecordRepository;
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
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final ModelMapper modelMapper;
    private final WebClient webClient;

    @Transactional
    public Mono<MedicalRecordDto> createMedicalRecord(CreateMedicalRecordRequest request) {
        log.info("Creating medical record for patient: {}", request.getPatientId());

        MedicalRecord record = MedicalRecord.builder()
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .recordDate(request.getRecordDate())
                .diagnosis(request.getDiagnosis())
                .symptoms(request.getSymptoms())
                .treatment(request.getTreatment())
                .notes(request.getNotes())
                .bloodPressure(request.getBloodPressure())
                .temperature(request.getTemperature())
                .heartRate(request.getHeartRate())
                .weight(request.getWeight())
                .height(request.getHeight())
                .active(true)
                .build();

        return medicalRecordRepository.save(record)
                .map(this::mapToDto)
                .doOnSuccess(dto -> {
                    log.info("Created medical record with ID: {}", dto.getId());
                    notifyMedicalRecordCreated(dto).subscribe();
                });
    }

    @Transactional(readOnly = true)
    public Mono<MedicalRecordDto> getMedicalRecordById(UUID id) {
        log.info("Fetching medical record by ID: {}", id);

        return medicalRecordRepository.findById(id)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Medical record", id)));
    }

    @Transactional(readOnly = true)
    public Flux<MedicalRecordDto> getAllMedicalRecords() {
        log.info("Fetching all medical records");
        return medicalRecordRepository.findByActiveTrue()
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<MedicalRecordDto> getMedicalRecordsByPatientId(UUID patientId) {
        log.info("Fetching medical records for patient: {}", patientId);
        return medicalRecordRepository.findByPatientIdAndActiveTrue(patientId)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<MedicalRecordDto> getMedicalRecordsByDoctorId(UUID doctorId) {
        log.info("Fetching medical records for doctor: {}", doctorId);
        return medicalRecordRepository.findByDoctorIdAndActiveTrue(doctorId)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<MedicalRecordDto> getMedicalRecordsByPatientIdAndDateRange(
            UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching medical records for patient {} between {} and {}", patientId, startDate, endDate);
        return medicalRecordRepository.findByPatientIdAndDateRange(patientId, startDate, endDate)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<MedicalRecordDto> searchByDiagnosis(UUID patientId, String diagnosis) {
        log.info("Searching medical records for patient {} with diagnosis containing: {}", patientId, diagnosis);
        return medicalRecordRepository.findByPatientIdAndDiagnosis(patientId, diagnosis)
                .map(this::mapToDto);
    }

    @Transactional
    public Mono<MedicalRecordDto> updateMedicalRecord(UUID id, UpdateMedicalRecordRequest request) {
        log.info("Updating medical record: {}", id);

        return medicalRecordRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Medical record", id)))
                .flatMap(record -> {
                    if (request.getDiagnosis() != null) {
                        record.setDiagnosis(request.getDiagnosis());
                    }
                    if (request.getSymptoms() != null) {
                        record.setSymptoms(request.getSymptoms());
                    }
                    if (request.getTreatment() != null) {
                        record.setTreatment(request.getTreatment());
                    }
                    if (request.getNotes() != null) {
                        record.setNotes(request.getNotes());
                    }
                    if (request.getBloodPressure() != null) {
                        record.setBloodPressure(request.getBloodPressure());
                    }
                    if (request.getTemperature() != null) {
                        record.setTemperature(request.getTemperature());
                    }
                    if (request.getHeartRate() != null) {
                        record.setHeartRate(request.getHeartRate());
                    }
                    if (request.getWeight() != null) {
                        record.setWeight(request.getWeight());
                    }
                    if (request.getHeight() != null) {
                        record.setHeight(request.getHeight());
                    }
                    return medicalRecordRepository.save(record);
                })
                .map(this::mapToDto)
                .doOnSuccess(dto -> log.info("Updated medical record: {}", dto.getId()));
    }

    @Transactional
    public Mono<Void> deleteMedicalRecord(UUID id) {
        log.info("Soft deleting medical record: {}", id);

        return medicalRecordRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Medical record", id)))
                .flatMap(record -> {
                    record.setActive(false);
                    return medicalRecordRepository.save(record);
                })
                .doOnSuccess(record -> log.info("Soft deleted medical record: {}", id))
                .then();
    }

    @Transactional(readOnly = true)
    public Mono<Long> countByPatientId(UUID patientId) {
        return medicalRecordRepository.countByPatientIdAndActiveTrue(patientId);
    }

    @Transactional(readOnly = true)
    public Mono<Long> countByDoctorId(UUID doctorId) {
        return medicalRecordRepository.countByDoctorIdAndActiveTrue(doctorId);
    }

    private MedicalRecordDto mapToDto(MedicalRecord record) {
        MedicalRecordDto dto = modelMapper.map(record, MedicalRecordDto.class);

        if (record.getPrescription() != null) {
            PrescriptionDto prescriptionDto = modelMapper.map(record.getPrescription(), PrescriptionDto.class);
            prescriptionDto.setMedicalRecordId(record.getId());
            dto.setPrescription(prescriptionDto);
        }

        if (record.getMedicalReport() != null) {
            MedicalReportDto reportDto = modelMapper.map(record.getMedicalReport(), MedicalReportDto.class);
            reportDto.setMedicalRecordId(record.getId());
            dto.setMedicalReport(reportDto);
        }

        return dto;
    }

    private Mono<Void> notifyMedicalRecordCreated(MedicalRecordDto record) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/medical-record-created")
                .bodyValue(record)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify medical record creation: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
