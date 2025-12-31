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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Prescription management APIs")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    @Operation(summary = "Create a new prescription")
    public ResponseEntity<ApiResponse<PrescriptionDto>> createPrescription(
            @Valid @RequestBody CreatePrescriptionRequest request) {

        log.info("Create prescription request for medical record {}", request.getMedicalRecordId());

        PrescriptionDto prescription = prescriptionService.createPrescription(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prescription created successfully", prescription));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prescription by ID")
    public ResponseEntity<ApiResponse<PrescriptionDto>> getPrescriptionById(@PathVariable UUID id) {
        log.info("Get prescription by ID: {}", id);

        PrescriptionDto prescription = prescriptionService.getPrescriptionById(id);

        return ResponseEntity.ok(ApiResponse.success(prescription));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get prescriptions by patient ID")
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getPrescriptionsByPatientId(
            @PathVariable UUID patientId) {
        log.info("Get prescriptions for patient ID: {}", patientId);

        List<PrescriptionDto> prescriptions = prescriptionService.getPrescriptionsByPatientId(patientId);

        return ResponseEntity.ok(ApiResponse.success(prescriptions));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get prescriptions by doctor ID")
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getPrescriptionsByDoctorId(
            @PathVariable UUID doctorId) {
        log.info("Get prescriptions for doctor ID: {}", doctorId);

        List<PrescriptionDto> prescriptions = prescriptionService.getPrescriptionsByDoctorId(doctorId);

        return ResponseEntity.ok(ApiResponse.success(prescriptions));
    }

    @GetMapping("/patient/{patientId}/date-range")
    @Operation(summary = "Get prescriptions by patient ID and date range")
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getPrescriptionsByDateRange(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Get prescriptions for patient {} between {} and {}", patientId, startDate, endDate);

        List<PrescriptionDto> prescriptions = prescriptionService.getPrescriptionsByPatientIdAndDateRange(
                patientId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(prescriptions));
    }

    @GetMapping("/patient/{patientId}/refillable")
    @Operation(summary = "Get refillable prescriptions for a patient")
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getRefillablePrescriptions(
            @PathVariable UUID patientId) {
        log.info("Get refillable prescriptions for patient ID: {}", patientId);

        List<PrescriptionDto> prescriptions = prescriptionService.getRefillablePrescriptions(patientId);

        return ResponseEntity.ok(ApiResponse.success(prescriptions));
    }

    @PostMapping("/{id}/refill")
    @Operation(summary = "Refill a prescription")
    public ResponseEntity<ApiResponse<PrescriptionDto>> refillPrescription(@PathVariable UUID id) {
        log.info("Refill prescription request for ID: {}", id);

        PrescriptionDto prescription = prescriptionService.refillPrescription(id);

        return ResponseEntity.ok(ApiResponse.success("Prescription refilled successfully", prescription));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete prescription (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deletePrescription(@PathVariable UUID id) {
        log.info("Delete prescription request for ID: {}", id);

        prescriptionService.deletePrescription(id);

        return ResponseEntity.ok(ApiResponse.success("Prescription deleted successfully", null));
    }
}
