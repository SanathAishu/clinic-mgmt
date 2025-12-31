package com.hospital.medicalrecords.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.medicalrecords.dto.*;
import com.hospital.medicalrecords.service.MedicalReportService;
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
@RequestMapping("/api/medical-reports")
@RequiredArgsConstructor
@Tag(name = "Medical Reports", description = "Medical Reports management APIs")
public class MedicalReportController {

    private final MedicalReportService medicalReportService;

    @PostMapping
    @Operation(summary = "Create a new medical report")
    public ResponseEntity<ApiResponse<MedicalReportDto>> createMedicalReport(
            @Valid @RequestBody CreateMedicalReportRequest request) {

        log.info("Create medical report request for medical record {}", request.getMedicalRecordId());

        MedicalReportDto report = medicalReportService.createMedicalReport(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Medical report created successfully", report));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get medical report by ID")
    public ResponseEntity<ApiResponse<MedicalReportDto>> getMedicalReportById(@PathVariable UUID id) {
        log.info("Get medical report by ID: {}", id);

        MedicalReportDto report = medicalReportService.getMedicalReportById(id);

        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get medical reports by patient ID")
    public ResponseEntity<ApiResponse<List<MedicalReportDto>>> getMedicalReportsByPatientId(
            @PathVariable UUID patientId) {
        log.info("Get medical reports for patient ID: {}", patientId);

        List<MedicalReportDto> reports = medicalReportService.getMedicalReportsByPatientId(patientId);

        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get medical reports by doctor ID")
    public ResponseEntity<ApiResponse<List<MedicalReportDto>>> getMedicalReportsByDoctorId(
            @PathVariable UUID doctorId) {
        log.info("Get medical reports for doctor ID: {}", doctorId);

        List<MedicalReportDto> reports = medicalReportService.getMedicalReportsByDoctorId(doctorId);

        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/patient/{patientId}/type/{reportType}")
    @Operation(summary = "Get medical reports by patient ID and type")
    public ResponseEntity<ApiResponse<List<MedicalReportDto>>> getMedicalReportsByType(
            @PathVariable UUID patientId,
            @PathVariable String reportType) {

        log.info("Get medical reports for patient {} with type: {}", patientId, reportType);

        List<MedicalReportDto> reports = medicalReportService.getMedicalReportsByPatientIdAndType(patientId, reportType);

        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/patient/{patientId}/date-range")
    @Operation(summary = "Get medical reports by patient ID and date range")
    public ResponseEntity<ApiResponse<List<MedicalReportDto>>> getMedicalReportsByDateRange(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Get medical reports for patient {} between {} and {}", patientId, startDate, endDate);

        List<MedicalReportDto> reports = medicalReportService.getMedicalReportsByPatientIdAndDateRange(
                patientId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete medical report (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteMedicalReport(@PathVariable UUID id) {
        log.info("Delete medical report request for ID: {}", id);

        medicalReportService.deleteMedicalReport(id);

        return ResponseEntity.ok(ApiResponse.success("Medical report deleted successfully", null));
    }
}
