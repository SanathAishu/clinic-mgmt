package com.hospital.doctor.controller;

import com.hospital.common.dto.ApiResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor", description = "Doctor management APIs")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new doctor")
    public Mono<ApiResponse<DoctorDto>> createDoctor(
            @Valid @RequestBody CreateDoctorRequest request) {
        log.info("Create doctor request for email: {}", request.getEmail());

        return doctorService.createDoctor(request)
                .map(doctor -> ApiResponse.success("Doctor created successfully", doctor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID")
    public Mono<ApiResponse<DoctorDto>> getDoctorById(@PathVariable UUID id) {
        log.info("Get doctor by ID: {}", id);

        return doctorService.getDoctorById(id)
                .map(ApiResponse::success);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get doctor by user ID")
    public Mono<ApiResponse<DoctorDto>> getDoctorByUserId(@PathVariable UUID userId) {
        log.info("Get doctor by user ID: {}", userId);

        return doctorService.getDoctorByUserId(userId)
                .map(ApiResponse::success);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get doctor by email")
    public Mono<ApiResponse<DoctorDto>> getDoctorByEmail(@PathVariable String email) {
        log.info("Get doctor by email: {}", email);

        return doctorService.getDoctorByEmail(email)
                .map(ApiResponse::success);
    }

    @GetMapping
    @Operation(summary = "Get all doctors")
    public Flux<DoctorDto> getAllDoctors() {
        log.info("Get all doctors");
        return doctorService.getAllDoctors();
    }

    @GetMapping("/active")
    @Operation(summary = "Get active doctors")
    public Flux<DoctorDto> getActiveDoctors() {
        log.info("Get active doctors");
        return doctorService.getActiveDoctors();
    }

    @GetMapping("/available")
    @Operation(summary = "Get available doctors")
    public Flux<DoctorDto> getAvailableDoctors() {
        log.info("Get available doctors");
        return doctorService.getAvailableDoctors();
    }

    @GetMapping("/search")
    @Operation(summary = "Search doctors by name or email")
    public Flux<DoctorDto> searchDoctors(@RequestParam String query) {
        log.info("Search doctors with query: {}", query);
        return doctorService.searchDoctors(query);
    }

    @GetMapping("/specialty/{specialty}")
    @Operation(summary = "Get doctors by specialty")
    public Flux<DoctorDto> getDoctorsBySpecialty(@PathVariable Specialty specialty) {
        log.info("Get doctors by specialty: {}", specialty);
        return doctorService.getDoctorsBySpecialty(specialty);
    }

    @GetMapping("/specialty/{specialty}/available")
    @Operation(summary = "Get available doctors by specialty (for appointments)")
    public Flux<DoctorDto> getAvailableDoctorsBySpecialty(@PathVariable Specialty specialty) {
        log.info("Get available doctors by specialty: {}", specialty);
        return doctorService.getAvailableDoctorsBySpecialty(specialty);
    }

    @GetMapping("/specialty/{specialty}/count")
    @Operation(summary = "Get doctor count by specialty")
    public Mono<ApiResponse<Long>> countBySpecialty(@PathVariable Specialty specialty) {
        return doctorService.countBySpecialty(specialty)
                .map(ApiResponse::success);
    }

    @GetMapping("/specialty/{specialty}/available/count")
    @Operation(summary = "Get available doctor count by specialty")
    public Mono<ApiResponse<Long>> countAvailableBySpecialty(@PathVariable Specialty specialty) {
        return doctorService.countAvailableBySpecialty(specialty)
                .map(ApiResponse::success);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update doctor")
    public Mono<ApiResponse<DoctorDto>> updateDoctor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDoctorRequest request) {
        log.info("Update doctor request for ID: {}", id);

        return doctorService.updateDoctor(id, request)
                .map(doctor -> ApiResponse.success("Doctor updated successfully", doctor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete doctor")
    public Mono<ApiResponse<Void>> deleteDoctor(@PathVariable UUID id) {
        log.info("Delete doctor request for ID: {}", id);

        return doctorService.deleteDoctor(id)
                .then(Mono.just(ApiResponse.success("Doctor deleted successfully", null)));
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Doctor Service is running");
    }
}
