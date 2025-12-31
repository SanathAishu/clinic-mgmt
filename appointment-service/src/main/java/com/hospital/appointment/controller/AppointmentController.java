package com.hospital.appointment.controller;

import com.hospital.appointment.dto.AppointmentDto;
import com.hospital.appointment.dto.CreateAppointmentRequest;
import com.hospital.appointment.dto.UpdateAppointmentRequest;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.common.dto.ApiResponse;
import com.hospital.common.dto.PageResponse;
import com.hospital.common.enums.AppointmentStatus;
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
 * Appointment REST controller
 */
@Slf4j
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment", description = "Appointment scheduling APIs")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Create a new appointment (with disease-specialty matching)")
    public ResponseEntity<ApiResponse<AppointmentDto>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {

        log.info("Create appointment request for patient {} with doctor {}", request.getPatientId(), request.getDoctorId());

        AppointmentDto appointment = appointmentService.createAppointment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment created successfully", appointment));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<ApiResponse<AppointmentDto>> getAppointmentById(@PathVariable UUID id) {
        log.info("Get appointment by ID: {}", id);

        AppointmentDto appointment = appointmentService.getAppointmentById(id);

        return ResponseEntity.ok(ApiResponse.success(appointment));
    }

    @GetMapping
    @Operation(summary = "Get all appointments with pagination")
    public ResponseEntity<PageResponse<AppointmentDto>> getAllAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appointmentDate") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AppointmentDto> appointmentsPage = appointmentService.getAllAppointments(pageable);

        PageResponse<AppointmentDto> response = PageResponse.<AppointmentDto>builder()
                .content(appointmentsPage.getContent())
                .pageNumber(appointmentsPage.getNumber())
                .pageSize(appointmentsPage.getSize())
                .totalElements(appointmentsPage.getTotalElements())
                .totalPages(appointmentsPage.getTotalPages())
                .last(appointmentsPage.isLast())
                .first(appointmentsPage.isFirst())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get appointments by patient ID")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointmentsByPatientId(@PathVariable UUID patientId) {
        log.info("Get appointments for patient ID: {}", patientId);

        List<AppointmentDto> appointments = appointmentService.getAppointmentsByPatientId(patientId);

        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get appointments by doctor ID")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointmentsByDoctorId(@PathVariable UUID doctorId) {
        log.info("Get appointments for doctor ID: {}", doctorId);

        List<AppointmentDto> appointments = appointmentService.getAppointmentsByDoctorId(doctorId);

        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get appointments by status")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointmentsByStatus(@PathVariable AppointmentStatus status) {
        log.info("Get appointments with status: {}", status);

        List<AppointmentDto> appointments = appointmentService.getAppointmentsByStatus(status);

        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @GetMapping("/status/{status}/count")
    @Operation(summary = "Get appointment count by status")
    public ResponseEntity<ApiResponse<Long>> countByStatus(@PathVariable AppointmentStatus status) {
        long count = appointmentService.countByStatus(status);

        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming appointments for reminders")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getUpcomingAppointments(
            @RequestParam(defaultValue = "24") int hoursAhead) {

        List<AppointmentDto> appointments = appointmentService.getUpcomingAppointments(hoursAhead);

        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update appointment")
    public ResponseEntity<ApiResponse<AppointmentDto>> updateAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequest request) {

        log.info("Update appointment request for ID: {}", id);

        AppointmentDto appointment = appointmentService.updateAppointment(id, request);

        return ResponseEntity.ok(ApiResponse.success("Appointment updated successfully", appointment));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel appointment")
    public ResponseEntity<ApiResponse<AppointmentDto>> cancelAppointment(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {

        log.info("Cancel appointment request for ID: {}", id);

        AppointmentDto appointment = appointmentService.cancelAppointment(id, reason);

        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully", appointment));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete appointment")
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(@PathVariable UUID id) {
        log.info("Delete appointment request for ID: {}", id);

        appointmentService.deleteAppointment(id);

        return ResponseEntity.ok(ApiResponse.success("Appointment deleted successfully", null));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Appointment Service is running");
    }
}
