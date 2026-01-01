package com.hospital.facility.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.common.dto.PageResponse;
import com.hospital.facility.dto.*;
import com.hospital.facility.entity.BookingStatus;
import com.hospital.facility.service.RoomBookingService;
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

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Room Bookings", description = "Room booking and admission management APIs")
public class RoomBookingController {

    private final RoomBookingService bookingService;

    @PostMapping("/admit")
    @Operation(summary = "Admit a patient to a room")
    public ResponseEntity<ApiResponse<RoomBookingDto>> admitPatient(
            @Valid @RequestBody AdmitPatientRequest request) {
        log.info("Admit patient request: patient {} to room {}", request.getPatientId(), request.getRoomId());
        RoomBookingDto booking = bookingService.admitPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient admitted successfully", booking));
    }

    @PostMapping("/{id}/discharge")
    @Operation(summary = "Discharge a patient")
    public ResponseEntity<ApiResponse<RoomBookingDto>> dischargePatient(
            @PathVariable UUID id,
            @RequestBody DischargePatientRequest request) {
        log.info("Discharge patient request for booking: {}", id);
        RoomBookingDto booking = bookingService.dischargePatient(id, request);
        return ResponseEntity.ok(ApiResponse.success("Patient discharged successfully", booking));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<RoomBookingDto>> cancelBooking(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        log.info("Cancel booking request: {}", id);
        RoomBookingDto booking = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", booking));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<ApiResponse<RoomBookingDto>> getBookingById(@PathVariable UUID id) {
        log.info("Get booking by ID: {}", id);
        RoomBookingDto booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get bookings by patient ID")
    public ResponseEntity<ApiResponse<List<RoomBookingDto>>> getBookingsByPatientId(
            @PathVariable UUID patientId) {
        log.info("Get bookings for patient: {}", patientId);
        List<RoomBookingDto> bookings = bookingService.getBookingsByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/patient/{patientId}/active")
    @Operation(summary = "Get active booking by patient ID")
    public ResponseEntity<ApiResponse<RoomBookingDto>> getActiveBookingByPatientId(
            @PathVariable UUID patientId) {
        log.info("Get active booking for patient: {}", patientId);
        RoomBookingDto booking = bookingService.getActiveBookingByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get bookings by room ID")
    public ResponseEntity<ApiResponse<List<RoomBookingDto>>> getBookingsByRoomId(
            @PathVariable UUID roomId) {
        log.info("Get bookings for room: {}", roomId);
        List<RoomBookingDto> bookings = bookingService.getBookingsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/room/{roomId}/active")
    @Operation(summary = "Get active bookings by room ID")
    public ResponseEntity<ApiResponse<List<RoomBookingDto>>> getActiveBookingsByRoomId(
            @PathVariable UUID roomId) {
        log.info("Get active bookings for room: {}", roomId);
        List<RoomBookingDto> bookings = bookingService.getActiveBookingsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get bookings by status with pagination")
    public ResponseEntity<PageResponse<RoomBookingDto>> getBookingsByStatus(
            @PathVariable BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "admissionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        log.info("Get bookings with status: {}", status);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<RoomBookingDto> bookingsPage = bookingService.getBookingsByStatus(status, pageable);
        PageResponse<RoomBookingDto> response = PageResponse.<RoomBookingDto>builder()
                .content(bookingsPage.getContent())
                .pageNumber(bookingsPage.getNumber())
                .pageSize(bookingsPage.getSize())
                .totalElements(bookingsPage.getTotalElements())
                .totalPages(bookingsPage.getTotalPages())
                .last(bookingsPage.isLast())
                .first(bookingsPage.isFirst())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/active")
    @Operation(summary = "Get count of active bookings")
    public ResponseEntity<ApiResponse<Long>> countActiveBookings() {
        long count = bookingService.countActiveBookings();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
