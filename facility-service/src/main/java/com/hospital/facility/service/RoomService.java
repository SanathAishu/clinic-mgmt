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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        log.info("Creating room: {}", request.getRoomNumber());

        if (roomRepository.findByRoomNumber(request.getRoomNumber()).isPresent()) {
            throw new ValidationException("Room number already exists: " + request.getRoomNumber());
        }

        Room room = Room.builder()
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

        room = roomRepository.save(room);
        log.info("Created room with ID: {}", room.getId());

        return mapToDto(room);
    }

    @Cacheable(value = "rooms", key = "#id")
    public RoomDto getRoomById(UUID id) {
        log.info("Fetching room by ID: {}", id);

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room", id));

        return mapToDto(room);
    }

    public RoomDto getRoomByNumber(String roomNumber) {
        log.info("Fetching room by number: {}", roomNumber);

        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new NotFoundException("Room with number: " + roomNumber));

        return mapToDto(room);
    }

    public Page<RoomDto> getAllRooms(Pageable pageable) {
        log.info("Fetching all rooms with pagination");
        return roomRepository.findByActiveTrue(pageable)
                .map(this::mapToDto);
    }

    public List<RoomDto> getAvailableRooms() {
        log.info("Fetching available rooms");
        return roomRepository.findByAvailableTrueAndActiveTrue()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<RoomDto> getAvailableRoomsByType(RoomType roomType) {
        log.info("Fetching available rooms of type: {}", roomType);
        return roomRepository.findAvailableRoomsByType(roomType)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<RoomDto> getRoomsByType(RoomType roomType) {
        log.info("Fetching rooms of type: {}", roomType);
        return roomRepository.findByRoomTypeAndActiveTrue(roomType)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<RoomDto> getRoomsByFloor(String floor) {
        log.info("Fetching rooms on floor: {}", floor);
        return roomRepository.findByFloor(floor)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CachePut(value = "rooms", key = "#id")
    public RoomDto updateRoom(UUID id, UpdateRoomRequest request) {
        log.info("Updating room: {}", id);

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room", id));

        if (request.getRoomType() != null) {
            room.setRoomType(request.getRoomType());
        }
        if (request.getCapacity() != null) {
            if (request.getCapacity() < room.getCurrentOccupancy()) {
                throw new ValidationException("Cannot reduce capacity below current occupancy");
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

        room = roomRepository.save(room);
        log.info("Updated room: {}", room.getId());

        return mapToDto(room);
    }

    @Transactional
    @CacheEvict(value = "rooms", key = "#id")
    public void deleteRoom(UUID id) {
        log.info("Soft deleting room: {}", id);

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room", id));

        if (room.getCurrentOccupancy() > 0) {
            throw new ValidationException("Cannot delete room with active occupants");
        }

        room.setActive(false);
        room.setAvailable(false);
        roomRepository.save(room);

        log.info("Soft deleted room: {}", id);
    }

    public long countAvailableRooms() {
        return roomRepository.countAvailableRooms();
    }

    public Long countAvailableBeds() {
        Long count = roomRepository.countAvailableBeds();
        return count != null ? count : 0L;
    }

    private RoomDto mapToDto(Room room) {
        RoomDto dto = modelMapper.map(room, RoomDto.class);
        dto.setAvailableBeds(room.getCapacity() - room.getCurrentOccupancy());
        return dto;
    }
}
