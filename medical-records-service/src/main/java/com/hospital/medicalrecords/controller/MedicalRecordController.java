package com.hospital.medicalrecords.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.medicalrecords.dto.*;
import com.hospital.medicalrecords.service.MedicalRecordService;
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
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
@Tag(name = "Medical Records", description = "Medical Records management APIs")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new medical record")
    public Mono<ApiResponse<MedicalRecordDto>> createMedicalRecord(
            @Valid @RequestBody CreateMedicalRecordRequest request) {

        log.info("Create medical record request for patient {}", request.getPatientId());

        return medicalRecordService.createMedicalRecord(request)
                .map(record -> ApiResponse.success("Medical record created successfully", record));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get medical record by ID")
    public Mono<ApiResponse<MedicalRecordDto>> getMedicalRecordById(@PathVariable UUID id) {
        log.info("Get medical record by ID: {}", id);

        return medicalRecordService.getMedicalRecordById(id)
                .map(ApiResponse::success);
    }

    @GetMapping
    @Operation(summary = "Get all medical records")
    public Flux<MedicalRecordDto> getAllMedicalRecords() {
        log.info("Get all medical records");
        return medicalRecordService.getAllMedicalRecords();
    }

    @GetMapping("/list")
    @Operation(summary = "Get all medical records as list")
    public Mono<ApiResponse<Flux<MedicalRecordDto>>> getAllMedicalRecordsWrapped() {
        log.info("Get all medical records wrapped");
        return Mono.just(ApiResponse.success(medicalRecordService.getAllMedicalRecords()));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get medical records by patient ID")
    public Flux<MedicalRecordDto> getMedicalRecordsByPatientId(@PathVariable UUID patientId) {
        log.info("Get medical records for patient ID: {}", patientId);
        return medicalRecordService.getMedicalRecordsByPatientId(patientId);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get medical records by doctor ID")
    public Flux<MedicalRecordDto> getMedicalRecordsByDoctorId(@PathVariable UUID doctorId) {
        log.info("Get medical records for doctor ID: {}", doctorId);
        return medicalRecordService.getMedicalRecordsByDoctorId(doctorId);
    }

    @GetMapping("/patient/{patientId}/date-range")
    @Operation(summary = "Get medical records by patient ID and date range")
    public Flux<MedicalRecordDto> getMedicalRecordsByDateRange(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Get medical records for patient {} between {} and {}", patientId, startDate, endDate);
        return medicalRecordService.getMedicalRecordsByPatientIdAndDateRange(patientId, startDate, endDate);
    }

    @GetMapping("/patient/{patientId}/search")
    @Operation(summary = "Search medical records by diagnosis")
    public Flux<MedicalRecordDto> searchByDiagnosis(
            @PathVariable UUID patientId,
            @RequestParam String diagnosis) {

        log.info("Search medical records for patient {} with diagnosis: {}", patientId, diagnosis);
        return medicalRecordService.searchByDiagnosis(patientId, diagnosis);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update medical record")
    public Mono<ApiResponse<MedicalRecordDto>> updateMedicalRecord(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicalRecordRequest request) {

        log.info("Update medical record request for ID: {}", id);

        return medicalRecordService.updateMedicalRecord(id, request)
                .map(record -> ApiResponse.success("Medical record updated successfully", record));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete medical record (soft delete)")
    public Mono<ApiResponse<Void>> deleteMedicalRecord(@PathVariable UUID id) {
        log.info("Delete medical record request for ID: {}", id);

        return medicalRecordService.deleteMedicalRecord(id)
                .then(Mono.just(ApiResponse.success("Medical record deleted successfully", null)));
    }

    @GetMapping("/patient/{patientId}/count")
    @Operation(summary = "Get medical record count by patient ID")
    public Mono<ApiResponse<Long>> countByPatientId(@PathVariable UUID patientId) {
        return medicalRecordService.countByPatientId(patientId)
                .map(ApiResponse::success);
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Medical Records Service is running");
    }
}
