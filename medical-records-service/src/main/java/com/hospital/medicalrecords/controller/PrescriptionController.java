package com.hospital.medicalrecords.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.medicalrecords.dto.*;
import com.hospital.medicalrecords.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Prescription management APIs")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new prescription")
    public Mono<ApiResponse<PrescriptionDto>> createPrescription(
            @Valid @RequestBody CreatePrescriptionRequest request) {

        log.info("Create prescription request for medical record {}", request.getMedicalRecordId());

        return prescriptionService.createPrescription(request)
                .map(prescription -> ApiResponse.success("Prescription created successfully", prescription));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prescription by ID")
    public Mono<ApiResponse<PrescriptionDto>> getPrescriptionById(@PathVariable UUID id) {
        log.info("Get prescription by ID: {}", id);

        return prescriptionService.getPrescriptionById(id)
                .map(ApiResponse::success);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get prescriptions by patient ID")
    public Flux<PrescriptionDto> getPrescriptionsByPatientId(@PathVariable UUID patientId) {
        log.info("Get prescriptions for patient ID: {}", patientId);
        return prescriptionService.getPrescriptionsByPatientId(patientId);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get prescriptions by doctor ID")
    public Flux<PrescriptionDto> getPrescriptionsByDoctorId(@PathVariable UUID doctorId) {
        log.info("Get prescriptions for doctor ID: {}", doctorId);
        return prescriptionService.getPrescriptionsByDoctorId(doctorId);
    }

    @GetMapping("/patient/{patientId}/date-range")
    @Operation(summary = "Get prescriptions by patient ID and date range")
    public Flux<PrescriptionDto> getPrescriptionsByDateRange(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Get prescriptions for patient {} between {} and {}", patientId, startDate, endDate);
        return prescriptionService.getPrescriptionsByPatientIdAndDateRange(patientId, startDate, endDate);
    }

    @GetMapping("/patient/{patientId}/refillable")
    @Operation(summary = "Get refillable prescriptions for a patient")
    public Flux<PrescriptionDto> getRefillablePrescriptions(@PathVariable UUID patientId) {
        log.info("Get refillable prescriptions for patient ID: {}", patientId);
        return prescriptionService.getRefillablePrescriptions(patientId);
    }

    @PostMapping("/{id}/refill")
    @Operation(summary = "Refill a prescription")
    public Mono<ApiResponse<PrescriptionDto>> refillPrescription(@PathVariable UUID id) {
        log.info("Refill prescription request for ID: {}", id);

        return prescriptionService.refillPrescription(id)
                .map(prescription -> ApiResponse.success("Prescription refilled successfully", prescription));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete prescription (soft delete)")
    public Mono<ApiResponse<Void>> deletePrescription(@PathVariable UUID id) {
        log.info("Delete prescription request for ID: {}", id);

        return prescriptionService.deletePrescription(id)
                .then(Mono.just(ApiResponse.success("Prescription deleted successfully", null)));
    }
}
