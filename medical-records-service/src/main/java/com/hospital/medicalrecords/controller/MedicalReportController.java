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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/medical-reports")
@RequiredArgsConstructor
@Tag(name = "Medical Reports", description = "Medical Reports management APIs")
public class MedicalReportController {

    private final MedicalReportService medicalReportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new medical report")
    public Mono<ApiResponse<MedicalReportDto>> createMedicalReport(
            @Valid @RequestBody CreateMedicalReportRequest request) {

        log.info("Create medical report request for medical record {}", request.getMedicalRecordId());

        return medicalReportService.createMedicalReport(request)
                .map(report -> ApiResponse.success("Medical report created successfully", report));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get medical report by ID")
    public Mono<ApiResponse<MedicalReportDto>> getMedicalReportById(@PathVariable UUID id) {
        log.info("Get medical report by ID: {}", id);

        return medicalReportService.getMedicalReportById(id)
                .map(ApiResponse::success);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get medical reports by patient ID")
    public Flux<MedicalReportDto> getMedicalReportsByPatientId(@PathVariable UUID patientId) {
        log.info("Get medical reports for patient ID: {}", patientId);
        return medicalReportService.getMedicalReportsByPatientId(patientId);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get medical reports by doctor ID")
    public Flux<MedicalReportDto> getMedicalReportsByDoctorId(@PathVariable UUID doctorId) {
        log.info("Get medical reports for doctor ID: {}", doctorId);
        return medicalReportService.getMedicalReportsByDoctorId(doctorId);
    }

    @GetMapping("/patient/{patientId}/type/{reportType}")
    @Operation(summary = "Get medical reports by patient ID and type")
    public Flux<MedicalReportDto> getMedicalReportsByType(
            @PathVariable UUID patientId,
            @PathVariable String reportType) {

        log.info("Get medical reports for patient {} with type: {}", patientId, reportType);
        return medicalReportService.getMedicalReportsByPatientIdAndType(patientId, reportType);
    }

    @GetMapping("/patient/{patientId}/date-range")
    @Operation(summary = "Get medical reports by patient ID and date range")
    public Flux<MedicalReportDto> getMedicalReportsByDateRange(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Get medical reports for patient {} between {} and {}", patientId, startDate, endDate);
        return medicalReportService.getMedicalReportsByPatientIdAndDateRange(patientId, startDate, endDate);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete medical report (soft delete)")
    public Mono<ApiResponse<Void>> deleteMedicalReport(@PathVariable UUID id) {
        log.info("Delete medical report request for ID: {}", id);

        return medicalReportService.deleteMedicalReport(id)
                .then(Mono.just(ApiResponse.success("Medical report deleted successfully", null)));
    }
}
