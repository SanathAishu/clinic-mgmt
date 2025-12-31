package com.hospital.medicalrecords.service;

import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.medicalrecords.dto.*;
import com.hospital.medicalrecords.entity.MedicalRecord;
import com.hospital.medicalrecords.entity.Prescription;
import com.hospital.medicalrecords.event.MedicalRecordEventPublisher;
import com.hospital.medicalrecords.repository.MedicalRecordRepository;
import com.hospital.medicalrecords.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    @Transactional
    public PrescriptionDto createPrescription(CreatePrescriptionRequest request) {
        log.info("Creating prescription for medical record: {}", request.getMedicalRecordId());

        MedicalRecord medicalRecord = medicalRecordRepository.findById(request.getMedicalRecordId())
                .orElseThrow(() -> new NotFoundException("Medical record", request.getMedicalRecordId()));

        if (medicalRecord.getPrescription() != null) {
            throw new ValidationException("Medical record already has a prescription");
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

        prescription = prescriptionRepository.save(prescription);
        eventPublisher.publishPrescriptionCreated(prescription);

        log.info("Created prescription with ID: {}", prescription.getId());
        return mapToDto(prescription);
    }

    @Cacheable(value = "prescriptions", key = "#id")
    public PrescriptionDto getPrescriptionById(UUID id) {
        log.info("Fetching prescription by ID: {}", id);

        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Prescription", id));

        return mapToDto(prescription);
    }

    public List<PrescriptionDto> getPrescriptionsByPatientId(UUID patientId) {
        log.info("Fetching prescriptions for patient: {}", patientId);
        return prescriptionRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PrescriptionDto> getPrescriptionsByDoctorId(UUID doctorId) {
        log.info("Fetching prescriptions for doctor: {}", doctorId);
        return prescriptionRepository.findByDoctorIdAndActiveTrue(doctorId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PrescriptionDto> getPrescriptionsByPatientIdAndDateRange(
            UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching prescriptions for patient {} between {} and {}", patientId, startDate, endDate);
        return prescriptionRepository.findByPatientIdAndDateRange(patientId, startDate, endDate)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PrescriptionDto> getRefillablePrescriptions(UUID patientId) {
        log.info("Fetching refillable prescriptions for patient: {}", patientId);
        return prescriptionRepository.findRefillablePrescriptions(patientId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "prescriptions", key = "#id")
    public PrescriptionDto refillPrescription(UUID id) {
        log.info("Refilling prescription: {}", id);

        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Prescription", id));

        if (!prescription.getRefillable()) {
            throw new ValidationException("Prescription is not refillable");
        }

        if (prescription.getRefillsRemaining() <= 0) {
            throw new ValidationException("No refills remaining for this prescription");
        }

        prescription.setRefillsRemaining(prescription.getRefillsRemaining() - 1);
        prescription.setPrescriptionDate(LocalDate.now());
        prescription = prescriptionRepository.save(prescription);

        log.info("Refilled prescription: {}, refills remaining: {}", id, prescription.getRefillsRemaining());
        return mapToDto(prescription);
    }

    @Transactional
    @CacheEvict(value = "prescriptions", key = "#id")
    public void deletePrescription(UUID id) {
        log.info("Soft deleting prescription: {}", id);

        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Prescription", id));

        prescription.setActive(false);
        prescriptionRepository.save(prescription);

        log.info("Soft deleted prescription: {}", id);
    }

    private PrescriptionDto mapToDto(Prescription prescription) {
        PrescriptionDto dto = modelMapper.map(prescription, PrescriptionDto.class);
        dto.setMedicalRecordId(prescription.getMedicalRecord().getId());
        return dto;
    }
}
