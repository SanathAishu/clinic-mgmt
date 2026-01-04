package com.hospital.appointment.controller;

import com.hospital.appointment.dto.AppointmentDto;
import com.hospital.appointment.dto.CreateAppointmentRequest;
import com.hospital.appointment.dto.UpdateAppointmentRequest;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.common.dto.ApiResponse;
import com.hospital.common.enums.AppointmentStatus;
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
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment", description = "Appointment scheduling APIs")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new appointment (with disease-specialty matching)")
    public Mono<ApiResponse<AppointmentDto>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        log.info("Create appointment request for patient {} with doctor {}", request.getPatientId(), request.getDoctorId());

        return appointmentService.createAppointment(request)
                .map(appointment -> ApiResponse.success("Appointment created successfully", appointment));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public Mono<ApiResponse<AppointmentDto>> getAppointmentById(@PathVariable UUID id) {
        log.info("Get appointment by ID: {}", id);

        return appointmentService.getAppointmentById(id)
                .map(ApiResponse::success);
    }

    @GetMapping
    @Operation(summary = "Get all appointments")
    public Flux<AppointmentDto> getAllAppointments() {
        log.info("Get all appointments");
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get appointments by patient ID")
    public Flux<AppointmentDto> getAppointmentsByPatientId(@PathVariable UUID patientId) {
        log.info("Get appointments for patient ID: {}", patientId);
        return appointmentService.getAppointmentsByPatientId(patientId);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get appointments by doctor ID")
    public Flux<AppointmentDto> getAppointmentsByDoctorId(@PathVariable UUID doctorId) {
        log.info("Get appointments for doctor ID: {}", doctorId);
        return appointmentService.getAppointmentsByDoctorId(doctorId);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get appointments by status")
    public Flux<AppointmentDto> getAppointmentsByStatus(@PathVariable AppointmentStatus status) {
        log.info("Get appointments with status: {}", status);
        return appointmentService.getAppointmentsByStatus(status);
    }

    @GetMapping("/status/{status}/count")
    @Operation(summary = "Get appointment count by status")
    public Mono<ApiResponse<Long>> countByStatus(@PathVariable AppointmentStatus status) {
        return appointmentService.countByStatus(status)
                .map(ApiResponse::success);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming appointments for reminders")
    public Flux<AppointmentDto> getUpcomingAppointments(
            @RequestParam(defaultValue = "24") int hoursAhead) {
        return appointmentService.getUpcomingAppointments(hoursAhead);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update appointment")
    public Mono<ApiResponse<AppointmentDto>> updateAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        log.info("Update appointment request for ID: {}", id);

        return appointmentService.updateAppointment(id, request)
                .map(appointment -> ApiResponse.success("Appointment updated successfully", appointment));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel appointment")
    public Mono<ApiResponse<AppointmentDto>> cancelAppointment(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        log.info("Cancel appointment request for ID: {}", id);

        return appointmentService.cancelAppointment(id, reason)
                .map(appointment -> ApiResponse.success("Appointment cancelled successfully", appointment));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete appointment")
    public Mono<ApiResponse<Void>> deleteAppointment(@PathVariable UUID id) {
        log.info("Delete appointment request for ID: {}", id);

        return appointmentService.deleteAppointment(id)
                .then(Mono.just(ApiResponse.success("Appointment deleted successfully", null)));
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Appointment Service is running");
    }
}
