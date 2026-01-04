package com.hospital.facility.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.facility.dto.*;
import com.hospital.facility.entity.BookingStatus;
import com.hospital.facility.service.RoomBookingService;
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
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Room Bookings", description = "Room booking and admission management APIs")
public class RoomBookingController {

    private final RoomBookingService bookingService;

    @PostMapping("/admit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Admit a patient to a room")
    public Mono<ApiResponse<RoomBookingDto>> admitPatient(
            @Valid @RequestBody AdmitPatientRequest request) {
        log.info("Admit patient request: patient {} to room {}", request.getPatientId(), request.getRoomId());
        return bookingService.admitPatient(request)
                .map(booking -> ApiResponse.success("Patient admitted successfully", booking));
    }

    @PostMapping("/{id}/discharge")
    @Operation(summary = "Discharge a patient")
    public Mono<ApiResponse<RoomBookingDto>> dischargePatient(
            @PathVariable UUID id,
            @RequestBody DischargePatientRequest request) {
        log.info("Discharge patient request for booking: {}", id);
        return bookingService.dischargePatient(id, request)
                .map(booking -> ApiResponse.success("Patient discharged successfully", booking));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public Mono<ApiResponse<RoomBookingDto>> cancelBooking(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        log.info("Cancel booking request: {}", id);
        return bookingService.cancelBooking(id, reason)
                .map(booking -> ApiResponse.success("Booking cancelled successfully", booking));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public Mono<ApiResponse<RoomBookingDto>> getBookingById(@PathVariable UUID id) {
        log.info("Get booking by ID: {}", id);
        return bookingService.getBookingById(id)
                .map(ApiResponse::success);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get bookings by patient ID")
    public Flux<RoomBookingDto> getBookingsByPatientId(@PathVariable UUID patientId) {
        log.info("Get bookings for patient: {}", patientId);
        return bookingService.getBookingsByPatientId(patientId);
    }

    @GetMapping("/patient/{patientId}/active")
    @Operation(summary = "Get active booking by patient ID")
    public Mono<ApiResponse<RoomBookingDto>> getActiveBookingByPatientId(
            @PathVariable UUID patientId) {
        log.info("Get active booking for patient: {}", patientId);
        return bookingService.getActiveBookingByPatientId(patientId)
                .map(ApiResponse::success);
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get bookings by room ID")
    public Flux<RoomBookingDto> getBookingsByRoomId(@PathVariable UUID roomId) {
        log.info("Get bookings for room: {}", roomId);
        return bookingService.getBookingsByRoomId(roomId);
    }

    @GetMapping("/room/{roomId}/active")
    @Operation(summary = "Get active bookings by room ID")
    public Flux<RoomBookingDto> getActiveBookingsByRoomId(@PathVariable UUID roomId) {
        log.info("Get active bookings for room: {}", roomId);
        return bookingService.getActiveBookingsByRoomId(roomId);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get bookings by status")
    public Flux<RoomBookingDto> getBookingsByStatus(@PathVariable BookingStatus status) {
        log.info("Get bookings with status: {}", status);
        return bookingService.getBookingsByStatus(status);
    }

    @GetMapping("/stats/active")
    @Operation(summary = "Get count of active bookings")
    public Mono<ApiResponse<Long>> countActiveBookings() {
        return bookingService.countActiveBookings()
                .map(ApiResponse::success);
    }
}
