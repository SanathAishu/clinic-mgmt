package com.hospital.facility.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.facility.dto.*;
import com.hospital.facility.entity.RoomType;
import com.hospital.facility.service.RoomService;
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
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management APIs")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new room")
    public Mono<ApiResponse<RoomDto>> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {
        log.info("Create room request: {}", request.getRoomNumber());
        return roomService.createRoom(request)
                .map(room -> ApiResponse.success("Room created successfully", room));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID")
    public Mono<ApiResponse<RoomDto>> getRoomById(@PathVariable UUID id) {
        log.info("Get room by ID: {}", id);
        return roomService.getRoomById(id)
                .map(ApiResponse::success);
    }

    @GetMapping("/number/{roomNumber}")
    @Operation(summary = "Get room by room number")
    public Mono<ApiResponse<RoomDto>> getRoomByNumber(@PathVariable String roomNumber) {
        log.info("Get room by number: {}", roomNumber);
        return roomService.getRoomByNumber(roomNumber)
                .map(ApiResponse::success);
    }

    @GetMapping
    @Operation(summary = "Get all rooms")
    public Mono<ApiResponse<Flux<RoomDto>>> getAllRooms() {
        log.info("Get all rooms");
        return Mono.just(ApiResponse.success(roomService.getAllRooms()));
    }

    @GetMapping("/list")
    @Operation(summary = "Get all rooms as list")
    public Flux<RoomDto> getAllRoomsFlux() {
        log.info("Get all rooms as flux");
        return roomService.getAllRooms();
    }

    @GetMapping("/available")
    @Operation(summary = "Get all available rooms")
    public Flux<RoomDto> getAvailableRooms() {
        log.info("Get available rooms");
        return roomService.getAvailableRooms();
    }

    @GetMapping("/available/type/{roomType}")
    @Operation(summary = "Get available rooms by type")
    public Flux<RoomDto> getAvailableRoomsByType(@PathVariable RoomType roomType) {
        log.info("Get available rooms of type: {}", roomType);
        return roomService.getAvailableRoomsByType(roomType);
    }

    @GetMapping("/type/{roomType}")
    @Operation(summary = "Get rooms by type")
    public Flux<RoomDto> getRoomsByType(@PathVariable RoomType roomType) {
        log.info("Get rooms of type: {}", roomType);
        return roomService.getRoomsByType(roomType);
    }

    @GetMapping("/floor/{floor}")
    @Operation(summary = "Get rooms by floor")
    public Flux<RoomDto> getRoomsByFloor(@PathVariable String floor) {
        log.info("Get rooms on floor: {}", floor);
        return roomService.getRoomsByFloor(floor);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room")
    public Mono<ApiResponse<RoomDto>> updateRoom(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest request) {
        log.info("Update room: {}", id);
        return roomService.updateRoom(id, request)
                .map(room -> ApiResponse.success("Room updated successfully", room));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room (soft delete)")
    public Mono<ApiResponse<Void>> deleteRoom(@PathVariable UUID id) {
        log.info("Delete room: {}", id);
        return roomService.deleteRoom(id)
                .then(Mono.just(ApiResponse.success("Room deleted successfully", null)));
    }

    @GetMapping("/stats/available-rooms")
    @Operation(summary = "Get count of available rooms")
    public Mono<ApiResponse<Long>> countAvailableRooms() {
        return roomService.countAvailableRooms()
                .map(ApiResponse::success);
    }

    @GetMapping("/stats/available-beds")
    @Operation(summary = "Get count of available beds")
    public Mono<ApiResponse<Long>> countAvailableBeds() {
        return roomService.countAvailableBeds()
                .map(ApiResponse::success);
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Facility Service is running");
    }
}
