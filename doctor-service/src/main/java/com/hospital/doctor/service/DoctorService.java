package com.hospital.doctor.service;

import com.hospital.common.enums.Specialty;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.doctor.dto.CreateDoctorRequest;
import com.hospital.doctor.dto.DoctorDto;
import com.hospital.doctor.dto.UpdateDoctorRequest;
import com.hospital.doctor.entity.Doctor;
import com.hospital.doctor.event.DoctorEventPublisher;
import com.hospital.doctor.repository.DoctorRepository;
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
 * Doctor service with CRUD operations, caching, and event publishing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    /**
     * Create a new doctor
     */
    @Transactional
    public DoctorDto createDoctor(CreateDoctorRequest request) {
        log.info("Creating doctor with email: {}", request.getEmail());

        // Validate unique constraints
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("email", "Email already exists");
        }

        if (doctorRepository.existsByUserId(request.getUserId())) {
            throw new ValidationException("userId", "User already has a doctor profile");
        }

        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ValidationException("licenseNumber", "License number already exists");
        }

        // Create doctor entity
        Doctor doctor = modelMapper.map(request, Doctor.class);
        if (request.getAvailable() == null) {
            doctor.setAvailable(true);
        }
        doctor = doctorRepository.save(doctor);

        log.info("Doctor created successfully with ID: {}", doctor.getId());

        // Publish DoctorCreatedEvent
        eventPublisher.publishDoctorCreated(doctor);

        return mapToDto(doctor);
    }

    /**
     * Get doctor by ID with caching
     */
    @Cacheable(value = "doctors", key = "#id")
    @Transactional(readOnly = true)
    public DoctorDto getDoctorById(UUID id) {
        log.info("Fetching doctor with ID: {}", id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor", id));

        return mapToDto(doctor);
    }

    /**
     * Get doctor by user ID
     */
    @Transactional(readOnly = true)
    public DoctorDto getDoctorByUserId(UUID userId) {
        log.info("Fetching doctor with user ID: {}", userId);

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Doctor with userId", userId));

        return mapToDto(doctor);
    }

    /**
     * Get doctor by email
     */
    @Transactional(readOnly = true)
    public DoctorDto getDoctorByEmail(String email) {
        log.info("Fetching doctor with email: {}", email);

        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Doctor with email", email));

        return mapToDto(doctor);
    }

    /**
     * Get all doctors with pagination
     */
    @Transactional(readOnly = true)
    public Page<DoctorDto> getAllDoctors(Pageable pageable) {
        log.info("Fetching all doctors, page: {}", pageable.getPageNumber());

        return doctorRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    /**
     * Get active doctors with pagination
     */
    @Transactional(readOnly = true)
    public Page<DoctorDto> getActiveDoctors(Pageable pageable) {
        log.info("Fetching active doctors, page: {}", pageable.getPageNumber());

        return doctorRepository.findActiveDoctors(pageable)
                .map(this::mapToDto);
    }

    /**
     * Get available doctors with pagination
     */
    @Transactional(readOnly = true)
    public Page<DoctorDto> getAvailableDoctors(Pageable pageable) {
        log.info("Fetching available doctors, page: {}", pageable.getPageNumber());

        return doctorRepository.findAvailableDoctors(pageable)
                .map(this::mapToDto);
    }

    /**
     * Search doctors by name or email
     */
    @Transactional(readOnly = true)
    public Page<DoctorDto> searchDoctors(String search, Pageable pageable) {
        log.info("Searching doctors with query: {}", search);

        return doctorRepository.searchDoctors(search, pageable)
                .map(this::mapToDto);
    }

    /**
     * Get doctors by specialty
     */
    @Transactional(readOnly = true)
    public List<DoctorDto> getDoctorsBySpecialty(Specialty specialty) {
        log.info("Fetching doctors with specialty: {}", specialty);

        return doctorRepository.findBySpecialty(specialty).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get available doctors by specialty (for appointments)
     */
    @Transactional(readOnly = true)
    public List<DoctorDto> getAvailableDoctorsBySpecialty(Specialty specialty) {
        log.info("Fetching available doctors with specialty: {}", specialty);

        return doctorRepository.findAvailableDoctorsBySpecialty(specialty).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Update doctor with cache update and event publishing
     */
    @CachePut(value = "doctors", key = "#id")
    @Transactional
    public DoctorDto updateDoctor(UUID id, UpdateDoctorRequest request) {
        log.info("Updating doctor with ID: {}", id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor", id));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(doctor.getEmail())) {
            if (doctorRepository.existsByEmail(request.getEmail())) {
                throw new ValidationException("email", "Email already exists");
            }
        }

        // Check license number uniqueness if changed
        if (request.getLicenseNumber() != null && !request.getLicenseNumber().equals(doctor.getLicenseNumber())) {
            if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                throw new ValidationException("licenseNumber", "License number already exists");
            }
        }

        // Update fields
        if (request.getName() != null) doctor.setName(request.getName());
        if (request.getEmail() != null) doctor.setEmail(request.getEmail());
        if (request.getPhone() != null) doctor.setPhone(request.getPhone());
        if (request.getGender() != null) doctor.setGender(request.getGender());
        if (request.getSpecialty() != null) doctor.setSpecialty(request.getSpecialty());
        if (request.getLicenseNumber() != null) doctor.setLicenseNumber(request.getLicenseNumber());
        if (request.getYearsOfExperience() != null) doctor.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getQualifications() != null) doctor.setQualifications(request.getQualifications());
        if (request.getBiography() != null) doctor.setBiography(request.getBiography());
        if (request.getClinicAddress() != null) doctor.setClinicAddress(request.getClinicAddress());
        if (request.getConsultationFee() != null) doctor.setConsultationFee(request.getConsultationFee());
        if (request.getAvailable() != null) doctor.setAvailable(request.getAvailable());
        if (request.getActive() != null) doctor.setActive(request.getActive());

        doctor = doctorRepository.save(doctor);

        log.info("Doctor updated successfully: {}", id);

        // Publish DoctorUpdatedEvent
        eventPublisher.publishDoctorUpdated(doctor);

        return mapToDto(doctor);
    }

    /**
     * Delete doctor with cache eviction and event publishing
     */
    @CacheEvict(value = "doctors", key = "#id")
    @Transactional
    public void deleteDoctor(UUID id) {
        log.info("Deleting doctor with ID: {}", id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor", id));

        doctorRepository.delete(doctor);

        log.info("Doctor deleted successfully: {}", id);

        // Publish DoctorDeletedEvent (not in common yet, but would follow same pattern)
        // eventPublisher.publishDoctorDeleted(id);
    }

    /**
     * Get doctor count by specialty
     */
    @Transactional(readOnly = true)
    public long countBySpecialty(Specialty specialty) {
        return doctorRepository.countBySpecialty(specialty);
    }

    /**
     * Get available doctor count by specialty
     */
    @Transactional(readOnly = true)
    public long countAvailableBySpecialty(Specialty specialty) {
        return doctorRepository.countAvailableBySpecialty(specialty);
    }

    /**
     * Map Doctor entity to DTO
     */
    private DoctorDto mapToDto(Doctor doctor) {
        return modelMapper.map(doctor, DoctorDto.class);
    }
}
