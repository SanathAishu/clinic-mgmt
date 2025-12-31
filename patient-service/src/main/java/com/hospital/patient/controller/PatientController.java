package com.hospital.patient.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.common.dto.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Patient REST controller
 */
@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient", description = "Patient management APIs")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @Operation(summary = "Create a new patient")
    public ResponseEntity<ApiResponse<PatientDto>> createPatient(
            @Valid @RequestBody CreatePatientRequest request) {

        log.info("Create patient request for email: {}", request.getEmail());

        PatientDto patient = patientService.createPatient(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient created successfully", patient));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<ApiResponse<PatientDto>> getPatientById(@PathVariable UUID id) {
        log.info("Get patient by ID: {}", id);

        PatientDto patient = patientService.getPatientById(id);

        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get patient by user ID")
    public ResponseEntity<ApiResponse<PatientDto>> getPatientByUserId(@PathVariable UUID userId) {
        log.info("Get patient by user ID: {}", userId);

        PatientDto patient = patientService.getPatientByUserId(userId);

        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get patient by email")
    public ResponseEntity<ApiResponse<PatientDto>> getPatientByEmail(@PathVariable String email) {
        log.info("Get patient by email: {}", email);

        PatientDto patient = patientService.getPatientByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @GetMapping
    @Operation(summary = "Get all patients with pagination")
    public ResponseEntity<PageResponse<PatientDto>> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("Get all patients, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<PatientDto> patientsPage = patientService.getAllPatients(pageable);

        PageResponse<PatientDto> response = PageResponse.<PatientDto>builder()
                .content(patientsPage.getContent())
                .pageNumber(patientsPage.getNumber())
                .pageSize(patientsPage.getSize())
                .totalElements(patientsPage.getTotalElements())
                .totalPages(patientsPage.getTotalPages())
                .last(patientsPage.isLast())
                .first(patientsPage.isFirst())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active patients")
    public ResponseEntity<PageResponse<PatientDto>> getActivePatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<PatientDto> patientsPage = patientService.getActivePatients(pageable);

        PageResponse<PatientDto> response = PageResponse.<PatientDto>builder()
                .content(patientsPage.getContent())
                .pageNumber(patientsPage.getNumber())
                .pageSize(patientsPage.getSize())
                .totalElements(patientsPage.getTotalElements())
                .totalPages(patientsPage.getTotalPages())
                .last(patientsPage.isLast())
                .first(patientsPage.isFirst())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search patients by name or email")
    public ResponseEntity<PageResponse<PatientDto>> searchPatients(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Search patients with query: {}", query);

        Pageable pageable = PageRequest.of(page, size);
        Page<PatientDto> patientsPage = patientService.searchPatients(query, pageable);

        PageResponse<PatientDto> response = PageResponse.<PatientDto>builder()
                .content(patientsPage.getContent())
                .pageNumber(patientsPage.getNumber())
                .pageSize(patientsPage.getSize())
                .totalElements(patientsPage.getTotalElements())
                .totalPages(patientsPage.getTotalPages())
                .last(patientsPage.isLast())
                .first(patientsPage.isFirst())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/disease/{disease}")
    @Operation(summary = "Get patients by disease")
    public ResponseEntity<ApiResponse<List<PatientDto>>> getPatientsByDisease(
            @PathVariable Disease disease) {

        log.info("Get patients by disease: {}", disease);

        List<PatientDto> patients = patientService.getPatientsByDisease(disease);

        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @GetMapping("/disease/{disease}/count")
    @Operation(summary = "Get patient count by disease")
    public ResponseEntity<ApiResponse<Long>> countByDisease(@PathVariable Disease disease) {
        long count = patientService.countByDisease(disease);

        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient")
    public ResponseEntity<ApiResponse<PatientDto>> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientRequest request) {

        log.info("Update patient request for ID: {}", id);

        PatientDto patient = patientService.updatePatient(id, request);

        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", patient));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient")
    public ResponseEntity<ApiResponse<Void>> deletePatient(@PathVariable UUID id) {
        log.info("Delete patient request for ID: {}", id);

        patientService.deletePatient(id);

        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully", null));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Patient Service is running");
    }
}
