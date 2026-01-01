package com.hospital.facility.controller;

import com.hospital.common.dto.ApiResponse;
import com.hospital.common.dto.PageResponse;
import com.hospital.facility.dto.*;
import com.hospital.facility.entity.RoomType;
import com.hospital.facility.service.RoomService;
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
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management APIs")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "Create a new room")
    public ResponseEntity<ApiResponse<RoomDto>> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {
        log.info("Create room request: {}", request.getRoomNumber());
        RoomDto room = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created successfully", room));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID")
    public ResponseEntity<ApiResponse<RoomDto>> getRoomById(@PathVariable UUID id) {
        log.info("Get room by ID: {}", id);
        RoomDto room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @GetMapping("/number/{roomNumber}")
    @Operation(summary = "Get room by room number")
    public ResponseEntity<ApiResponse<RoomDto>> getRoomByNumber(@PathVariable String roomNumber) {
        log.info("Get room by number: {}", roomNumber);
        RoomDto room = roomService.getRoomByNumber(roomNumber);
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @GetMapping
    @Operation(summary = "Get all rooms with pagination")
    public ResponseEntity<PageResponse<RoomDto>> getAllRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "roomNumber") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<RoomDto> roomsPage = roomService.getAllRooms(pageable);
        PageResponse<RoomDto> response = PageResponse.<RoomDto>builder()
                .content(roomsPage.getContent())
                .pageNumber(roomsPage.getNumber())
                .pageSize(roomsPage.getSize())
                .totalElements(roomsPage.getTotalElements())
                .totalPages(roomsPage.getTotalPages())
                .last(roomsPage.isLast())
                .first(roomsPage.isFirst())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    @Operation(summary = "Get all available rooms")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAvailableRooms() {
        log.info("Get available rooms");
        List<RoomDto> rooms = roomService.getAvailableRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/available/type/{roomType}")
    @Operation(summary = "Get available rooms by type")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAvailableRoomsByType(
            @PathVariable RoomType roomType) {
        log.info("Get available rooms of type: {}", roomType);
        List<RoomDto> rooms = roomService.getAvailableRoomsByType(roomType);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/type/{roomType}")
    @Operation(summary = "Get rooms by type")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getRoomsByType(@PathVariable RoomType roomType) {
        log.info("Get rooms of type: {}", roomType);
        List<RoomDto> rooms = roomService.getRoomsByType(roomType);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/floor/{floor}")
    @Operation(summary = "Get rooms by floor")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getRoomsByFloor(@PathVariable String floor) {
        log.info("Get rooms on floor: {}", floor);
        List<RoomDto> rooms = roomService.getRoomsByFloor(floor);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room")
    public ResponseEntity<ApiResponse<RoomDto>> updateRoom(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest request) {
        log.info("Update room: {}", id);
        RoomDto room = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", room));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable UUID id) {
        log.info("Delete room: {}", id);
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }

    @GetMapping("/stats/available-rooms")
    @Operation(summary = "Get count of available rooms")
    public ResponseEntity<ApiResponse<Long>> countAvailableRooms() {
        long count = roomService.countAvailableRooms();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/stats/available-beds")
    @Operation(summary = "Get count of available beds")
    public ResponseEntity<ApiResponse<Long>> countAvailableBeds() {
        Long count = roomService.countAvailableBeds();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Facility Service is running");
    }
}
