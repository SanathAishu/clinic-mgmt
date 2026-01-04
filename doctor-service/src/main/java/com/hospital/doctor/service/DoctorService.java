package com.hospital.doctor.service;

import com.hospital.common.enums.Specialty;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.doctor.dto.CreateDoctorRequest;
import com.hospital.doctor.dto.DoctorDto;
import com.hospital.doctor.dto.UpdateDoctorRequest;
import com.hospital.doctor.entity.Doctor;
import com.hospital.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;
    private final WebClient webClient;

    @Transactional
    public Mono<DoctorDto> createDoctor(CreateDoctorRequest request) {
        log.info("Creating doctor with email: {}", request.getEmail());

        return doctorRepository.existsByEmail(request.getEmail())
                .flatMap(emailExists -> {
                    if (Boolean.TRUE.equals(emailExists)) {
                        return Mono.error(new ValidationException("email", "Email already exists"));
                    }
                    return doctorRepository.existsByUserId(request.getUserId());
                })
                .flatMap(userExists -> {
                    if (Boolean.TRUE.equals(userExists)) {
                        return Mono.error(new ValidationException("userId", "User already has a doctor profile"));
                    }
                    return doctorRepository.existsByLicenseNumber(request.getLicenseNumber());
                })
                .flatMap(licenseExists -> {
                    if (Boolean.TRUE.equals(licenseExists)) {
                        return Mono.error(new ValidationException("licenseNumber", "License number already exists"));
                    }
                    Doctor doctor = modelMapper.map(request, Doctor.class);
                    doctor.setId(UUID.randomUUID());
                    doctor.setNew(true);
                    if (request.getAvailable() == null) {
                        doctor.setAvailable(true);
                    }
                    return doctorRepository.save(doctor);
                })
                .map(this::mapToDto)
                .doOnSuccess(dto -> {
                    log.info("Doctor created successfully with ID: {}", dto.getId());
                    notifyDoctorCreated(dto).subscribe();
                });
    }

    @Transactional(readOnly = true)
    public Mono<DoctorDto> getDoctorById(UUID id) {
        log.info("Fetching doctor with ID: {}", id);

        return doctorRepository.findById(id)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Doctor", id)));
    }

    @Transactional(readOnly = true)
    public Mono<DoctorDto> getDoctorByUserId(UUID userId) {
        log.info("Fetching doctor with user ID: {}", userId);

        return doctorRepository.findByUserId(userId)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Doctor with userId", userId)));
    }

    @Transactional(readOnly = true)
    public Mono<DoctorDto> getDoctorByEmail(String email) {
        log.info("Fetching doctor with email: {}", email);

        return doctorRepository.findByEmail(email)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Doctor with email", email)));
    }

    @Transactional(readOnly = true)
    public Flux<DoctorDto> getAllDoctors() {
        log.info("Fetching all doctors");

        return doctorRepository.findAll()
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<DoctorDto> getActiveDoctors() {
        log.info("Fetching active doctors");

        return doctorRepository.findActiveDoctors()
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<DoctorDto> getAvailableDoctors() {
        log.info("Fetching available doctors");

        return doctorRepository.findAvailableDoctors()
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<DoctorDto> searchDoctors(String search) {
        log.info("Searching doctors with query: {}", search);

        return doctorRepository.searchDoctors(search)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<DoctorDto> getDoctorsBySpecialty(Specialty specialty) {
        log.info("Fetching doctors with specialty: {}", specialty);

        return doctorRepository.findBySpecialty(specialty)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<DoctorDto> getAvailableDoctorsBySpecialty(Specialty specialty) {
        log.info("Fetching available doctors with specialty: {}", specialty);

        return doctorRepository.findAvailableDoctorsBySpecialty(specialty)
                .map(this::mapToDto);
    }

    @Transactional
    public Mono<DoctorDto> updateDoctor(UUID id, UpdateDoctorRequest request) {
        log.info("Updating doctor with ID: {}", id);

        return doctorRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Doctor", id)))
                .flatMap(doctor -> validateEmailUpdate(doctor, request))
                .flatMap(doctor -> validateLicenseUpdate(doctor, request))
                .flatMap(doctor -> {
                    updateDoctorFields(doctor, request);
                    return doctorRepository.save(doctor);
                })
                .map(this::mapToDto)
                .doOnSuccess(dto -> {
                    log.info("Doctor updated successfully: {}", id);
                    notifyDoctorUpdated(dto).subscribe();
                });
    }

    private Mono<Doctor> validateEmailUpdate(Doctor doctor, UpdateDoctorRequest request) {
        if (request.getEmail() != null && !request.getEmail().equals(doctor.getEmail())) {
            return doctorRepository.existsByEmail(request.getEmail())
                    .flatMap(exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            return Mono.error(new ValidationException("email", "Email already exists"));
                        }
                        return Mono.just(doctor);
                    });
        }
        return Mono.just(doctor);
    }

    private Mono<Doctor> validateLicenseUpdate(Doctor doctor, UpdateDoctorRequest request) {
        if (request.getLicenseNumber() != null && !request.getLicenseNumber().equals(doctor.getLicenseNumber())) {
            return doctorRepository.existsByLicenseNumber(request.getLicenseNumber())
                    .flatMap(exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            return Mono.error(new ValidationException("licenseNumber", "License number already exists"));
                        }
                        return Mono.just(doctor);
                    });
        }
        return Mono.just(doctor);
    }

    @Transactional
    public Mono<Void> deleteDoctor(UUID id) {
        log.info("Deleting doctor with ID: {}", id);

        return doctorRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Doctor", id)))
                .flatMap(doctor -> doctorRepository.delete(doctor)
                        .then(Mono.fromRunnable(() -> {
                            log.info("Doctor deleted successfully: {}", id);
                            notifyDoctorDeleted(id).subscribe();
                        })));
    }

    @Transactional(readOnly = true)
    public Mono<Long> countBySpecialty(Specialty specialty) {
        return doctorRepository.countBySpecialty(specialty);
    }

    @Transactional(readOnly = true)
    public Mono<Long> countAvailableBySpecialty(Specialty specialty) {
        return doctorRepository.countAvailableBySpecialty(specialty);
    }

    private void updateDoctorFields(Doctor doctor, UpdateDoctorRequest request) {
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
    }

    private DoctorDto mapToDto(Doctor doctor) {
        return modelMapper.map(doctor, DoctorDto.class);
    }

    private Mono<Void> notifyDoctorCreated(DoctorDto doctor) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/doctor-registered")
                .bodyValue(doctor)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify doctor creation: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> notifyDoctorUpdated(DoctorDto doctor) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/doctor-updated")
                .bodyValue(doctor)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify doctor update: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> notifyDoctorDeleted(UUID doctorId) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/doctor-deleted")
                .bodyValue(doctorId)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify doctor deletion: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
