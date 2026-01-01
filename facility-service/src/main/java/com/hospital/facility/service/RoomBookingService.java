package com.hospital.facility.service;

import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.facility.dto.*;
import com.hospital.facility.entity.BookingStatus;
import com.hospital.facility.entity.Room;
import com.hospital.facility.entity.RoomBooking;
import com.hospital.facility.event.FacilityEventPublisher;
import com.hospital.facility.repository.RoomBookingRepository;
import com.hospital.facility.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomBookingService {

    private final RoomBookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final FacilityEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    @Transactional
    public RoomBookingDto admitPatient(AdmitPatientRequest request) {
        log.info("Admitting patient {} to room {}", request.getPatientId(), request.getRoomId());

        // Check if patient already has active booking
        bookingRepository.findActiveBookingByPatientId(request.getPatientId())
                .ifPresent(b -> {
                    throw new ValidationException("Patient already has an active room booking");
                });

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room", request.getRoomId()));

        if (!room.hasAvailableBeds()) {
            throw new ValidationException("Room " + room.getRoomNumber() + " has no available beds");
        }

        if (!room.getActive()) {
            throw new ValidationException("Room is not active");
        }

        // Create booking with PENDING status (Saga pattern - step 1)
        RoomBooking booking = RoomBooking.builder()
                .room(room)
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .admissionDate(request.getAdmissionDate() != null ? request.getAdmissionDate() : LocalDate.now())
                .status(BookingStatus.CONFIRMED)  // For now, directly confirm (simplified saga)
                .admissionReason(request.getAdmissionReason())
                .notes(request.getNotes())
                .build();

        // Update room occupancy
        room.incrementOccupancy();
        roomRepository.save(room);

        booking = bookingRepository.save(booking);

        // Publish event
        eventPublisher.publishPatientAdmitted(booking, room);

        log.info("Patient {} admitted to room {} with booking ID: {}",
                request.getPatientId(), room.getRoomNumber(), booking.getId());

        return mapToDto(booking);
    }

    @Transactional
    public RoomBookingDto dischargePatient(UUID bookingId, DischargePatientRequest request) {
        log.info("Discharging patient from booking: {}", bookingId);

        RoomBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Room booking", bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Booking is not active, cannot discharge");
        }

        Room room = booking.getRoom();

        // Update booking
        booking.setStatus(BookingStatus.DISCHARGED);
        booking.setDischargeDate(request.getDischargeDate() != null ? request.getDischargeDate() : LocalDate.now());
        booking.setDischargeNotes(request.getDischargeNotes());

        // Update room occupancy
        room.decrementOccupancy();
        roomRepository.save(room);

        booking = bookingRepository.save(booking);

        // Publish event
        eventPublisher.publishPatientDischarged(booking, room);

        log.info("Patient discharged from room {} with booking ID: {}",
                room.getRoomNumber(), booking.getId());

        return mapToDto(booking);
    }

    @Transactional
    public RoomBookingDto cancelBooking(UUID bookingId, String reason) {
        log.info("Cancelling booking: {}", bookingId);

        RoomBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Room booking", bookingId));

        if (booking.getStatus() == BookingStatus.DISCHARGED) {
            throw new ValidationException("Cannot cancel a discharged booking");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            // If patient was already admitted, release the bed
            Room room = booking.getRoom();
            room.decrementOccupancy();
            roomRepository.save(room);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setNotes(reason != null ? reason : "Booking cancelled");
        booking = bookingRepository.save(booking);

        log.info("Booking {} cancelled", bookingId);

        return mapToDto(booking);
    }

    public RoomBookingDto getBookingById(UUID id) {
        log.info("Fetching booking by ID: {}", id);

        RoomBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room booking", id));

        return mapToDto(booking);
    }

    public List<RoomBookingDto> getBookingsByPatientId(UUID patientId) {
        log.info("Fetching bookings for patient: {}", patientId);
        return bookingRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public RoomBookingDto getActiveBookingByPatientId(UUID patientId) {
        log.info("Fetching active booking for patient: {}", patientId);
        return bookingRepository.findActiveBookingByPatientId(patientId)
                .map(this::mapToDto)
                .orElse(null);
    }

    public List<RoomBookingDto> getBookingsByRoomId(UUID roomId) {
        log.info("Fetching bookings for room: {}", roomId);
        return bookingRepository.findByRoomId(roomId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<RoomBookingDto> getActiveBookingsByRoomId(UUID roomId) {
        log.info("Fetching active bookings for room: {}", roomId);
        return bookingRepository.findActiveBookingsByRoomId(roomId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public Page<RoomBookingDto> getBookingsByStatus(BookingStatus status, Pageable pageable) {
        log.info("Fetching bookings with status: {}", status);
        return bookingRepository.findByStatus(status, pageable)
                .map(this::mapToDto);
    }

    public long countActiveBookings() {
        return bookingRepository.countActiveBookings();
    }

    private RoomBookingDto mapToDto(RoomBooking booking) {
        RoomBookingDto dto = modelMapper.map(booking, RoomBookingDto.class);
        dto.setRoomId(booking.getRoom().getId());
        dto.setRoomNumber(booking.getRoom().getRoomNumber());
        return dto;
    }
}
