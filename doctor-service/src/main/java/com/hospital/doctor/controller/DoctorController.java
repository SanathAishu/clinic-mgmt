package com.hospital.doctor.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.common.dto.PageResponse;
import com.hospital.common.enums.Specialty;
import com.hospital.doctor.dto.CreateDoctorRequest;
import com.hospital.doctor.dto.DoctorDto;
import com.hospital.doctor.dto.UpdateDoctorRequest;
import com.hospital.doctor.service.DoctorService;
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
 * Doctor REST controller
 */
@Slf4j
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor", description = "Doctor management APIs")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @Operation(summary = "Create a new doctor")
    public ResponseEntity<ApiResponse<DoctorDto>> createDoctor(
            @Valid @RequestBody CreateDoctorRequest request) {

        log.info("Create doctor request for email: {}", request.getEmail());

        DoctorDto doctor = doctorService.createDoctor(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Doctor created successfully", doctor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID")
    public ResponseEntity<ApiResponse<DoctorDto>> getDoctorById(@PathVariable UUID id) {
        log.info("Get doctor by ID: {}", id);

        DoctorDto doctor = doctorService.getDoctorById(id);

        return ResponseEntity.ok(ApiResponse.success(doctor));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get doctor by user ID")
    public ResponseEntity<ApiResponse<DoctorDto>> getDoctorByUserId(@PathVariable UUID userId) {
        log.info("Get doctor by user ID: {}", userId);

        DoctorDto doctor = doctorService.getDoctorByUserId(userId);

        return ResponseEntity.ok(ApiResponse.success(doctor));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get doctor by email")
    public ResponseEntity<ApiResponse<DoctorDto>> getDoctorByEmail(@PathVariable String email) {
        log.info("Get doctor by email: {}", email);

        DoctorDto doctor = doctorService.getDoctorByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(doctor));
    }

    @GetMapping
    @Operation(summary = "Get all doctors with pagination")
    public ResponseEntity<PageResponse<DoctorDto>> getAllDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("Get all doctors, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<DoctorDto> doctorsPage = doctorService.getAllDoctors(pageable);

        PageResponse<DoctorDto> response = PageResponse.<DoctorDto>builder()
                .content(doctorsPage.getContent())
                .pageNumber(doctorsPage.getNumber())
                .pageSize(doctorsPage.getSize())
                .totalElements(doctorsPage.getTotalElements())
                .totalPages(doctorsPage.getTotalPages())
                .last(doctorsPage.isLast())
                .first(doctorsPage.isFirst())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active doctors")
    public ResponseEntity<PageResponse<DoctorDto>> getActiveDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<DoctorDto> doctorsPage = doctorService.getActiveDoctors(pageable);

        PageResponse<DoctorDto> response = PageResponse.<DoctorDto>builder()
                .content(doctorsPage.getContent())
                .pageNumber(doctorsPage.getNumber())
                .pageSize(doctorsPage.getSize())
                .totalElements(doctorsPage.getTotalElements())
                .totalPages(doctorsPage.getTotalPages())
                .last(doctorsPage.isLast())
                .first(doctorsPage.isFirst())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available doctors")
    public ResponseEntity<PageResponse<DoctorDto>> getAvailableDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<DoctorDto> doctorsPage = doctorService.getAvailableDoctors(pageable);

        PageResponse<DoctorDto> response = PageResponse.<DoctorDto>builder()
                .content(doctorsPage.getContent())
                .pageNumber(doctorsPage.getNumber())
                .pageSize(doctorsPage.getSize())
                .totalElements(doctorsPage.getTotalElements())
                .totalPages(doctorsPage.getTotalPages())
                .last(doctorsPage.isLast())
                .first(doctorsPage.isFirst())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search doctors by name or email")
    public ResponseEntity<PageResponse<DoctorDto>> searchDoctors(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Search doctors with query: {}", query);

        Pageable pageable = PageRequest.of(page, size);
        Page<DoctorDto> doctorsPage = doctorService.searchDoctors(query, pageable);

        PageResponse<DoctorDto> response = PageResponse.<DoctorDto>builder()
                .content(doctorsPage.getContent())
                .pageNumber(doctorsPage.getNumber())
                .pageSize(doctorsPage.getSize())
                .totalElements(doctorsPage.getTotalElements())
                .totalPages(doctorsPage.getTotalPages())
                .last(doctorsPage.isLast())
                .first(doctorsPage.isFirst())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/specialty/{specialty}")
    @Operation(summary = "Get doctors by specialty")
    public ResponseEntity<ApiResponse<List<DoctorDto>>> getDoctorsBySpecialty(
            @PathVariable Specialty specialty) {

        log.info("Get doctors by specialty: {}", specialty);

        List<DoctorDto> doctors = doctorService.getDoctorsBySpecialty(specialty);

        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @GetMapping("/specialty/{specialty}/available")
    @Operation(summary = "Get available doctors by specialty (for appointments)")
    public ResponseEntity<ApiResponse<List<DoctorDto>>> getAvailableDoctorsBySpecialty(
            @PathVariable Specialty specialty) {

        log.info("Get available doctors by specialty: {}", specialty);

        List<DoctorDto> doctors = doctorService.getAvailableDoctorsBySpecialty(specialty);

        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @GetMapping("/specialty/{specialty}/count")
    @Operation(summary = "Get doctor count by specialty")
    public ResponseEntity<ApiResponse<Long>> countBySpecialty(@PathVariable Specialty specialty) {
        long count = doctorService.countBySpecialty(specialty);

        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/specialty/{specialty}/available/count")
    @Operation(summary = "Get available doctor count by specialty")
    public ResponseEntity<ApiResponse<Long>> countAvailableBySpecialty(@PathVariable Specialty specialty) {
        long count = doctorService.countAvailableBySpecialty(specialty);

        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update doctor")
    public ResponseEntity<ApiResponse<DoctorDto>> updateDoctor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDoctorRequest request) {

        log.info("Update doctor request for ID: {}", id);

        DoctorDto doctor = doctorService.updateDoctor(id, request);

        return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully", doctor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete doctor")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable UUID id) {
        log.info("Delete doctor request for ID: {}", id);

        doctorService.deleteDoctor(id);

        return ResponseEntity.ok(ApiResponse.success("Doctor deleted successfully", null));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Doctor Service is running");
    }
}
