package com.hospital.facility.service;

import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.facility.dto.*;
import com.hospital.facility.entity.Room;
import com.hospital.facility.entity.RoomType;
import com.hospital.facility.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final WebClient webClient;

    @Transactional
    public Mono<RoomDto> createRoom(CreateRoomRequest request) {
        log.info("Creating room: {}", request.getRoomNumber());

        return roomRepository.findByRoomNumber(request.getRoomNumber())
                .flatMap(existingRoom -> Mono.<Room>error(
                        new ValidationException("Room number already exists: " + request.getRoomNumber())))
                .switchIfEmpty(Mono.defer(() -> {
                    Room room = Room.builder()
                            .id(UUID.randomUUID())
                            .isNew(true)
                            .roomNumber(request.getRoomNumber())
                            .roomType(request.getRoomType())
                            .capacity(request.getCapacity())
                            .currentOccupancy(0)
                            .dailyRate(request.getDailyRate())
                            .floor(request.getFloor())
                            .wing(request.getWing())
                            .description(request.getDescription())
                            .available(true)
                            .active(true)
                            .build();
                    return roomRepository.save(room);
                }))
                .map(this::mapToDto)
                .doOnSuccess(dto -> log.info("Created room with ID: {}", dto.getId()));
    }

    @Transactional(readOnly = true)
    public Mono<RoomDto> getRoomById(UUID id) {
        log.info("Fetching room by ID: {}", id);

        return roomRepository.findById(id)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Room", id)));
    }

    @Transactional(readOnly = true)
    public Mono<RoomDto> getRoomByNumber(String roomNumber) {
        log.info("Fetching room by number: {}", roomNumber);

        return roomRepository.findByRoomNumber(roomNumber)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Room with number: " + roomNumber)));
    }

    @Transactional(readOnly = true)
    public Flux<RoomDto> getAllRooms() {
        log.info("Fetching all rooms");
        return roomRepository.findByActiveTrue()
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<RoomDto> getAvailableRooms() {
        log.info("Fetching available rooms");
        return roomRepository.findByAvailableTrueAndActiveTrue()
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<RoomDto> getAvailableRoomsByType(RoomType roomType) {
        log.info("Fetching available rooms of type: {}", roomType);
        return roomRepository.findAvailableRoomsByType(roomType)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<RoomDto> getRoomsByType(RoomType roomType) {
        log.info("Fetching rooms of type: {}", roomType);
        return roomRepository.findByRoomTypeAndActiveTrue(roomType)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<RoomDto> getRoomsByFloor(String floor) {
        log.info("Fetching rooms on floor: {}", floor);
        return roomRepository.findByFloor(floor)
                .map(this::mapToDto);
    }

    @Transactional
    public Mono<RoomDto> updateRoom(UUID id, UpdateRoomRequest request) {
        log.info("Updating room: {}", id);

        return roomRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Room", id)))
                .flatMap(room -> {
                    if (request.getRoomType() != null) {
                        room.setRoomType(request.getRoomType());
                    }
                    if (request.getCapacity() != null) {
                        if (request.getCapacity() < room.getCurrentOccupancy()) {
                            return Mono.error(new ValidationException("Cannot reduce capacity below current occupancy"));
                        }
                        room.setCapacity(request.getCapacity());
                    }
                    if (request.getDailyRate() != null) {
                        room.setDailyRate(request.getDailyRate());
                    }
                    if (request.getFloor() != null) {
                        room.setFloor(request.getFloor());
                    }
                    if (request.getWing() != null) {
                        room.setWing(request.getWing());
                    }
                    if (request.getDescription() != null) {
                        room.setDescription(request.getDescription());
                    }
                    if (request.getAvailable() != null) {
                        room.setAvailable(request.getAvailable());
                    }
                    return roomRepository.save(room);
                })
                .map(this::mapToDto)
                .doOnSuccess(dto -> log.info("Updated room: {}", dto.getId()));
    }

    @Transactional
    public Mono<Void> deleteRoom(UUID id) {
        log.info("Soft deleting room: {}", id);

        return roomRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Room", id)))
                .flatMap(room -> {
                    if (room.getCurrentOccupancy() > 0) {
                        return Mono.error(new ValidationException("Cannot delete room with active occupants"));
                    }
                    room.setActive(false);
                    room.setAvailable(false);
                    return roomRepository.save(room);
                })
                .doOnSuccess(room -> log.info("Soft deleted room: {}", id))
                .then();
    }

    @Transactional(readOnly = true)
    public Mono<Long> countAvailableRooms() {
        return roomRepository.countAvailableRooms()
                .defaultIfEmpty(0L);
    }

    @Transactional(readOnly = true)
    public Mono<Long> countAvailableBeds() {
        return roomRepository.countAvailableBeds()
                .defaultIfEmpty(0L);
    }

    private RoomDto mapToDto(Room room) {
        RoomDto dto = modelMapper.map(room, RoomDto.class);
        dto.setAvailableBeds(room.getCapacity() - room.getCurrentOccupancy());
        return dto;
    }
}
