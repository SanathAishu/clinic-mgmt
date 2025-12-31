package com.hospital.patient.service;

import com.hospital.common.enums.Disease;
import com.hospital.common.events.PatientCreatedEvent;
import com.hospital.common.events.PatientDeletedEvent;
import com.hospital.common.events.PatientUpdatedEvent;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.patient.dto.CreatePatientRequest;
import com.hospital.patient.dto.PatientDto;
import com.hospital.patient.dto.UpdatePatientRequest;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.event.PatientEventPublisher;
import com.hospital.patient.repository.PatientRepository;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Patient service with CRUD operations, caching, and event publishing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    /**
     * Create a new patient
     */
    @Transactional
    public PatientDto createPatient(CreatePatientRequest request) {
        log.info("Creating patient with email: {}", request.getEmail());

        // Validate unique constraints
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("email", "Email already exists");
        }

        if (patientRepository.existsByUserId(request.getUserId())) {
            throw new ValidationException("userId", "User already has a patient profile");
        }

        // Create patient entity
        Patient patient = modelMapper.map(request, Patient.class);
        patient = patientRepository.save(patient);

        log.info("Patient created successfully with ID: {}", patient.getId());

        // Publish PatientCreatedEvent
        eventPublisher.publishPatientCreated(patient);

        return mapToDto(patient);
    }

    /**
     * Get patient by ID with caching
     */
    @Cacheable(value = "patients", key = "#id")
    @Transactional(readOnly = true)
    public PatientDto getPatientById(UUID id) {
        log.info("Fetching patient with ID: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient", id));

        return mapToDto(patient);
    }

    /**
     * Get patient by user ID
     */
    @Transactional(readOnly = true)
    public PatientDto getPatientByUserId(UUID userId) {
        log.info("Fetching patient with user ID: {}", userId);

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Patient with userId", userId));

        return mapToDto(patient);
    }

    /**
     * Get patient by email
     */
    @Transactional(readOnly = true)
    public PatientDto getPatientByEmail(String email) {
        log.info("Fetching patient with email: {}", email);

        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Patient with email", email));

        return mapToDto(patient);
    }

    /**
     * Get all patients with pagination
     */
    @Transactional(readOnly = true)
    public Page<PatientDto> getAllPatients(Pageable pageable) {
        log.info("Fetching all patients, page: {}", pageable.getPageNumber());

        return patientRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    /**
     * Get active patients with pagination
     */
    @Transactional(readOnly = true)
    public Page<PatientDto> getActivePatients(Pageable pageable) {
        log.info("Fetching active patients, page: {}", pageable.getPageNumber());

        return patientRepository.findActivePatients(pageable)
                .map(this::mapToDto);
    }

    /**
     * Search patients by name or email
     */
    @Transactional(readOnly = true)
    public Page<PatientDto> searchPatients(String search, Pageable pageable) {
        log.info("Searching patients with query: {}", search);

        return patientRepository.searchPatients(search, pageable)
                .map(this::mapToDto);
    }

    /**
     * Get patients by disease
     */
    @Transactional(readOnly = true)
    public List<PatientDto> getPatientsByDisease(Disease disease) {
        log.info("Fetching patients with disease: {}", disease);

        return patientRepository.findByDisease(disease).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Update patient with cache update and event publishing
     */
    @CachePut(value = "patients", key = "#id")
    @Transactional
    public PatientDto updatePatient(UUID id, UpdatePatientRequest request) {
        log.info("Updating patient with ID: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient", id));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(patient.getEmail())) {
            if (patientRepository.existsByEmail(request.getEmail())) {
                throw new ValidationException("email", "Email already exists");
            }
        }

        // Update fields
        if (request.getName() != null) patient.setName(request.getName());
        if (request.getEmail() != null) patient.setEmail(request.getEmail());
        if (request.getPhone() != null) patient.setPhone(request.getPhone());
        if (request.getGender() != null) patient.setGender(request.getGender());
        if (request.getDateOfBirth() != null) patient.setDateOfBirth(request.getDateOfBirth());
        if (request.getAddress() != null) patient.setAddress(request.getAddress());
        if (request.getDisease() != null) patient.setDisease(request.getDisease());
        if (request.getMedicalHistory() != null) patient.setMedicalHistory(request.getMedicalHistory());
        if (request.getEmergencyContact() != null) patient.setEmergencyContact(request.getEmergencyContact());
        if (request.getEmergencyPhone() != null) patient.setEmergencyPhone(request.getEmergencyPhone());
        if (request.getActive() != null) patient.setActive(request.getActive());

        patient = patientRepository.save(patient);

        log.info("Patient updated successfully: {}", id);

        // Publish PatientUpdatedEvent
        eventPublisher.publishPatientUpdated(patient);

        return mapToDto(patient);
    }

    /**
     * Delete patient with cache eviction and event publishing
     */
    @CacheEvict(value = "patients", key = "#id")
    @Transactional
    public void deletePatient(UUID id) {
        log.info("Deleting patient with ID: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient", id));

        patientRepository.delete(patient);

        log.info("Patient deleted successfully: {}", id);

        // Publish PatientDeletedEvent
        eventPublisher.publishPatientDeleted(id);
    }

    /**
     * Get patient count by disease
     */
    @Transactional(readOnly = true)
    public long countByDisease(Disease disease) {
        return patientRepository.countByDisease(disease);
    }

    /**
     * Map Patient entity to DTO
     */
    private PatientDto mapToDto(Patient patient) {
        PatientDto dto = modelMapper.map(patient, PatientDto.class);
        dto.setAge(patient.getAge());
        return dto;
    }
}
