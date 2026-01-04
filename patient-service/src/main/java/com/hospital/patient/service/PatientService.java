package com.hospital.patient.service;

import com.hospital.common.enums.Disease;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.patient.dto.CreatePatientRequest;
import com.hospital.patient.dto.PatientDto;
import com.hospital.patient.dto.UpdatePatientRequest;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.repository.PatientRepository;
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
public class PatientService {

    private final PatientRepository patientRepository;
    private final ModelMapper modelMapper;
    private final WebClient webClient;

    @Transactional
    public Mono<PatientDto> createPatient(CreatePatientRequest request) {
        log.info("Creating patient with email: {}", request.getEmail());

        return patientRepository.existsByEmail(request.getEmail())
                .flatMap(emailExists -> {
                    if (Boolean.TRUE.equals(emailExists)) {
                        return Mono.error(new ValidationException("email", "Email already exists"));
                    }
                    return patientRepository.existsByUserId(request.getUserId());
                })
                .flatMap(userExists -> {
                    if (Boolean.TRUE.equals(userExists)) {
                        return Mono.error(new ValidationException("userId", "User already has a patient profile"));
                    }
                    Patient patient = modelMapper.map(request, Patient.class);
                    patient.setId(UUID.randomUUID());
                    patient.setNew(true);
                    return patientRepository.save(patient);
                })
                .map(this::mapToDto)
                .doOnSuccess(dto -> {
                    log.info("Patient created successfully with ID: {}", dto.getId());
                    notifyPatientCreated(dto).subscribe();
                });
    }

    @Transactional(readOnly = true)
    public Mono<PatientDto> getPatientById(UUID id) {
        log.info("Fetching patient with ID: {}", id);

        return patientRepository.findById(id)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Patient", id)));
    }

    @Transactional(readOnly = true)
    public Mono<PatientDto> getPatientByUserId(UUID userId) {
        log.info("Fetching patient with user ID: {}", userId);

        return patientRepository.findByUserId(userId)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Patient with userId", userId)));
    }

    @Transactional(readOnly = true)
    public Mono<PatientDto> getPatientByEmail(String email) {
        log.info("Fetching patient with email: {}", email);

        return patientRepository.findByEmail(email)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Patient with email", email)));
    }

    @Transactional(readOnly = true)
    public Flux<PatientDto> getAllPatients() {
        log.info("Fetching all patients");

        return patientRepository.findAll()
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<PatientDto> getActivePatients() {
        log.info("Fetching active patients");

        return patientRepository.findActivePatients()
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<PatientDto> searchPatients(String search) {
        log.info("Searching patients with query: {}", search);

        return patientRepository.searchPatients(search)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<PatientDto> getPatientsByDisease(Disease disease) {
        log.info("Fetching patients with disease: {}", disease);

        return patientRepository.findByDisease(disease)
                .map(this::mapToDto);
    }

    @Transactional
    public Mono<PatientDto> updatePatient(UUID id, UpdatePatientRequest request) {
        log.info("Updating patient with ID: {}", id);

        return patientRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Patient", id)))
                .flatMap(patient -> {
                    if (request.getEmail() != null && !request.getEmail().equals(patient.getEmail())) {
                        return patientRepository.existsByEmail(request.getEmail())
                                .flatMap(exists -> {
                                    if (Boolean.TRUE.equals(exists)) {
                                        return Mono.error(new ValidationException("email", "Email already exists"));
                                    }
                                    return Mono.just(patient);
                                });
                    }
                    return Mono.just(patient);
                })
                .flatMap(patient -> {
                    updatePatientFields(patient, request);
                    return patientRepository.save(patient);
                })
                .map(this::mapToDto)
                .doOnSuccess(dto -> {
                    log.info("Patient updated successfully: {}", id);
                    notifyPatientUpdated(dto).subscribe();
                });
    }

    @Transactional
    public Mono<Void> deletePatient(UUID id) {
        log.info("Deleting patient with ID: {}", id);

        return patientRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Patient", id)))
                .flatMap(patient -> patientRepository.delete(patient)
                        .then(Mono.fromRunnable(() -> {
                            log.info("Patient deleted successfully: {}", id);
                            notifyPatientDeleted(id).subscribe();
                        })));
    }

    @Transactional(readOnly = true)
    public Mono<Long> countByDisease(Disease disease) {
        return patientRepository.countByDisease(disease);
    }

    private void updatePatientFields(Patient patient, UpdatePatientRequest request) {
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
    }

    private PatientDto mapToDto(Patient patient) {
        PatientDto dto = modelMapper.map(patient, PatientDto.class);
        dto.setAge(patient.getAge());
        return dto;
    }

    private Mono<Void> notifyPatientCreated(PatientDto patient) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/patient-registered")
                .bodyValue(patient)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify patient creation: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> notifyPatientUpdated(PatientDto patient) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/patient-updated")
                .bodyValue(patient)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify patient update: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> notifyPatientDeleted(UUID patientId) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/patient-deleted")
                .bodyValue(patientId)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify patient deletion: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
