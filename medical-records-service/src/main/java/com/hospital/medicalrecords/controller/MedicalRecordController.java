package com.hospital.medicalrecords.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.common.dto.PageResponse;
import com.hospital.medicalrecords.dto.*;
import com.hospital.medicalrecords.service.MedicalRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
@Tag(name = "Medical Records", description = "Medical Records management APIs")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    @Operation(summary = "Create a new medical record")
    public ResponseEntity<ApiResponse<MedicalRecordDto>> createMedicalRecord(
            @Valid @RequestBody CreateMedicalRecordRequest request) {

        log.info("Create medical record request for patient {}", request.getPatientId());

        MedicalRecordDto record = medicalRecordService.createMedicalRecord(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Medical record created successfully", record));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get medical record by ID")
    public ResponseEntity<ApiResponse<MedicalRecordDto>> getMedicalRecordById(@PathVariable UUID id) {
        log.info("Get medical record by ID: {}", id);

        MedicalRecordDto record = medicalRecordService.getMedicalRecordById(id);

        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @GetMapping
    @Operation(summary = "Get all medical records with pagination")
    public ResponseEntity<PageResponse<MedicalRecordDto>> getAllMedicalRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recordDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<MedicalRecordDto> recordsPage = medicalRecordService.getAllMedicalRecords(pageable);

        PageResponse<MedicalRecordDto> response = PageResponse.<MedicalRecordDto>builder()
                .content(recordsPage.getContent())
                .pageNumber(recordsPage.getNumber())
                .pageSize(recordsPage.getSize())
                .totalElements(recordsPage.getTotalElements())
                .totalPages(recordsPage.getTotalPages())
                .last(recordsPage.isLast())
                .first(recordsPage.isFirst())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get medical records by patient ID")
    public ResponseEntity<ApiResponse<List<MedicalRecordDto>>> getMedicalRecordsByPatientId(
            @PathVariable UUID patientId) {
        log.info("Get medical records for patient ID: {}", patientId);

        List<MedicalRecordDto> records = medicalRecordService.getMedicalRecordsByPatientId(patientId);

        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get medical records by doctor ID")
    public ResponseEntity<ApiResponse<List<MedicalRecordDto>>> getMedicalRecordsByDoctorId(
            @PathVariable UUID doctorId) {
        log.info("Get medical records for doctor ID: {}", doctorId);

        List<MedicalRecordDto> records = medicalRecordService.getMedicalRecordsByDoctorId(doctorId);

        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/patient/{patientId}/date-range")
    @Operation(summary = "Get medical records by patient ID and date range")
    public ResponseEntity<ApiResponse<List<MedicalRecordDto>>> getMedicalRecordsByDateRange(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Get medical records for patient {} between {} and {}", patientId, startDate, endDate);

        List<MedicalRecordDto> records = medicalRecordService.getMedicalRecordsByPatientIdAndDateRange(
                patientId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/patient/{patientId}/search")
    @Operation(summary = "Search medical records by diagnosis")
    public ResponseEntity<ApiResponse<List<MedicalRecordDto>>> searchByDiagnosis(
            @PathVariable UUID patientId,
            @RequestParam String diagnosis) {

        log.info("Search medical records for patient {} with diagnosis: {}", patientId, diagnosis);

        List<MedicalRecordDto> records = medicalRecordService.searchByDiagnosis(patientId, diagnosis);

        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update medical record")
    public ResponseEntity<ApiResponse<MedicalRecordDto>> updateMedicalRecord(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicalRecordRequest request) {

        log.info("Update medical record request for ID: {}", id);

        MedicalRecordDto record = medicalRecordService.updateMedicalRecord(id, request);

        return ResponseEntity.ok(ApiResponse.success("Medical record updated successfully", record));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete medical record (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteMedicalRecord(@PathVariable UUID id) {
        log.info("Delete medical record request for ID: {}", id);

        medicalRecordService.deleteMedicalRecord(id);

        return ResponseEntity.ok(ApiResponse.success("Medical record deleted successfully", null));
    }

    @GetMapping("/patient/{patientId}/count")
    @Operation(summary = "Get medical record count by patient ID")
    public ResponseEntity<ApiResponse<Long>> countByPatientId(@PathVariable UUID patientId) {
        long count = medicalRecordService.countByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Medical Records Service is running");
    }
}
