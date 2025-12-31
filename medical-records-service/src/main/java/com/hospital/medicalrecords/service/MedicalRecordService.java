package com.hospital.medicalrecords.service;

import com.hospital.common.exception.NotFoundException;
import com.hospital.medicalrecords.dto.*;
import com.hospital.medicalrecords.entity.MedicalRecord;
import com.hospital.medicalrecords.event.MedicalRecordEventPublisher;
import com.hospital.medicalrecords.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    @Transactional
    public MedicalRecordDto createMedicalRecord(CreateMedicalRecordRequest request) {
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

        record = medicalRecordRepository.save(record);
        eventPublisher.publishMedicalRecordCreated(record);

        log.info("Created medical record with ID: {}", record.getId());
        return mapToDto(record);
    }

    @Cacheable(value = "medicalRecords", key = "#id")
    public MedicalRecordDto getMedicalRecordById(UUID id) {
        log.info("Fetching medical record by ID: {}", id);

        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical record", id));

        return mapToDto(record);
    }

    public Page<MedicalRecordDto> getAllMedicalRecords(Pageable pageable) {
        log.info("Fetching all medical records with pagination");
        return medicalRecordRepository.findByActiveTrue(pageable)
                .map(this::mapToDto);
    }

    public List<MedicalRecordDto> getMedicalRecordsByPatientId(UUID patientId) {
        log.info("Fetching medical records for patient: {}", patientId);
        return medicalRecordRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MedicalRecordDto> getMedicalRecordsByDoctorId(UUID doctorId) {
        log.info("Fetching medical records for doctor: {}", doctorId);
        return medicalRecordRepository.findByDoctorIdAndActiveTrue(doctorId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MedicalRecordDto> getMedicalRecordsByPatientIdAndDateRange(
            UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching medical records for patient {} between {} and {}", patientId, startDate, endDate);
        return medicalRecordRepository.findByPatientIdAndDateRange(patientId, startDate, endDate)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MedicalRecordDto> searchByDiagnosis(UUID patientId, String diagnosis) {
        log.info("Searching medical records for patient {} with diagnosis containing: {}", patientId, diagnosis);
        return medicalRecordRepository.findByPatientIdAndDiagnosis(patientId, diagnosis)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CachePut(value = "medicalRecords", key = "#id")
    public MedicalRecordDto updateMedicalRecord(UUID id, UpdateMedicalRecordRequest request) {
        log.info("Updating medical record: {}", id);

        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical record", id));

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

        record = medicalRecordRepository.save(record);
        log.info("Updated medical record: {}", record.getId());

        return mapToDto(record);
    }

    @Transactional
    @CacheEvict(value = "medicalRecords", key = "#id")
    public void deleteMedicalRecord(UUID id) {
        log.info("Soft deleting medical record: {}", id);

        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical record", id));

        record.setActive(false);
        medicalRecordRepository.save(record);

        log.info("Soft deleted medical record: {}", id);
    }

    public long countByPatientId(UUID patientId) {
        return medicalRecordRepository.countByPatientIdAndActiveTrue(patientId);
    }

    public long countByDoctorId(UUID doctorId) {
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
}
