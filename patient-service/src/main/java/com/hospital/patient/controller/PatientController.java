package com.hospital.patient.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.common.enums.Disease;
import com.hospital.patient.dto.CreatePatientRequest;
import com.hospital.patient.dto.PatientDto;
import com.hospital.patient.dto.UpdatePatientRequest;
import com.hospital.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient", description = "Patient management APIs")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new patient")
    public Mono<ApiResponse<PatientDto>> createPatient(
            @Valid @RequestBody CreatePatientRequest request) {
        log.info("Create patient request for email: {}", request.getEmail());

        return patientService.createPatient(request)
                .map(patient -> ApiResponse.success("Patient created successfully", patient));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public Mono<ApiResponse<PatientDto>> getPatientById(@PathVariable UUID id) {
        log.info("Get patient by ID: {}", id);

        return patientService.getPatientById(id)
                .map(ApiResponse::success);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get patient by user ID")
    public Mono<ApiResponse<PatientDto>> getPatientByUserId(@PathVariable UUID userId) {
        log.info("Get patient by user ID: {}", userId);

        return patientService.getPatientByUserId(userId)
                .map(ApiResponse::success);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get patient by email")
    public Mono<ApiResponse<PatientDto>> getPatientByEmail(@PathVariable String email) {
        log.info("Get patient by email: {}", email);

        return patientService.getPatientByEmail(email)
                .map(ApiResponse::success);
    }

    @GetMapping
    @Operation(summary = "Get all patients")
    public Mono<ApiResponse<Flux<PatientDto>>> getAllPatients() {
        log.info("Get all patients");

        return Mono.just(ApiResponse.success(patientService.getAllPatients()));
    }

    @GetMapping("/list")
    @Operation(summary = "Get all patients as list")
    public Flux<PatientDto> getAllPatientsFlux() {
        log.info("Get all patients as flux");
        return patientService.getAllPatients();
    }

    @GetMapping("/active")
    @Operation(summary = "Get active patients")
    public Flux<PatientDto> getActivePatients() {
        return patientService.getActivePatients();
    }

    @GetMapping("/search")
    @Operation(summary = "Search patients by name or email")
    public Flux<PatientDto> searchPatients(@RequestParam String query) {
        log.info("Search patients with query: {}", query);
        return patientService.searchPatients(query);
    }

    @GetMapping("/disease/{disease}")
    @Operation(summary = "Get patients by disease")
    public Flux<PatientDto> getPatientsByDisease(@PathVariable Disease disease) {
        log.info("Get patients by disease: {}", disease);
        return patientService.getPatientsByDisease(disease);
    }

    @GetMapping("/disease/{disease}/count")
    @Operation(summary = "Get patient count by disease")
    public Mono<ApiResponse<Long>> countByDisease(@PathVariable Disease disease) {
        return patientService.countByDisease(disease)
                .map(ApiResponse::success);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient")
    public Mono<ApiResponse<PatientDto>> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientRequest request) {
        log.info("Update patient request for ID: {}", id);

        return patientService.updatePatient(id, request)
                .map(patient -> ApiResponse.success("Patient updated successfully", patient));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient")
    public Mono<ApiResponse<Void>> deletePatient(@PathVariable UUID id) {
        log.info("Delete patient request for ID: {}", id);

        return patientService.deletePatient(id)
                .then(Mono.just(ApiResponse.success("Patient deleted successfully", null)));
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Patient Service is running");
    }
}
