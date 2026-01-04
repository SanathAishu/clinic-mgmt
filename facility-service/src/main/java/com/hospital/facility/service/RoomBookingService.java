package com.hospital.facility.service;

import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.facility.dto.*;
import com.hospital.facility.entity.BookingStatus;
import com.hospital.facility.entity.Room;
import com.hospital.facility.entity.RoomBooking;
import com.hospital.facility.repository.RoomBookingRepository;
import com.hospital.facility.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomBookingService {

    private final RoomBookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final WebClient webClient;

    @Transactional
    public Mono<RoomBookingDto> admitPatient(AdmitPatientRequest request) {
        log.info("Admitting patient {} to room {}", request.getPatientId(), request.getRoomId());

        return bookingRepository.findActiveBookingByPatientId(request.getPatientId())
                .flatMap(existingBooking -> Mono.<RoomBookingDto>error(
                        new ValidationException("Patient already has an active room booking")))
                .switchIfEmpty(Mono.defer(() -> roomRepository.findById(request.getRoomId())
                        .switchIfEmpty(Mono.error(new NotFoundException("Room", request.getRoomId())))
                        .flatMap(room -> {
                            if (!room.hasAvailableBeds()) {
                                return Mono.error(new ValidationException("Room " + room.getRoomNumber() + " has no available beds"));
                            }
                            if (!room.getActive()) {
                                return Mono.error(new ValidationException("Room is not active"));
                            }

                            RoomBooking booking = RoomBooking.builder()
                                    .roomId(room.getId())
                                    .patientId(request.getPatientId())
                                    .doctorId(request.getDoctorId())
                                    .admissionDate(request.getAdmissionDate() != null ? request.getAdmissionDate() : LocalDate.now())
                                    .status(BookingStatus.CONFIRMED)
                                    .admissionReason(request.getAdmissionReason())
                                    .notes(request.getNotes())
                                    .build();

                            room.incrementOccupancy();
                            return roomRepository.save(room)
                                    .then(bookingRepository.save(booking))
                                    .flatMap(savedBooking -> mapToDtoWithRoom(savedBooking, room));
                        })))
                .doOnSuccess(dto -> {
                    log.info("Patient {} admitted to room {} with booking ID: {}",
                            request.getPatientId(), dto.getRoomNumber(), dto.getId());
                    notifyPatientAdmitted(dto).subscribe();
                });
    }

    @Transactional
    public Mono<RoomBookingDto> dischargePatient(UUID bookingId, DischargePatientRequest request) {
        log.info("Discharging patient from booking: {}", bookingId);

        return bookingRepository.findById(bookingId)
                .switchIfEmpty(Mono.error(new NotFoundException("Room booking", bookingId)))
                .flatMap(booking -> {
                    if (booking.getStatus() != BookingStatus.CONFIRMED) {
                        return Mono.error(new ValidationException("Booking is not active, cannot discharge"));
                    }

                    return roomRepository.findById(booking.getRoomId())
                            .switchIfEmpty(Mono.error(new NotFoundException("Room", booking.getRoomId())))
                            .flatMap(room -> {
                                booking.setStatus(BookingStatus.DISCHARGED);
                                booking.setDischargeDate(request.getDischargeDate() != null ? request.getDischargeDate() : LocalDate.now());
                                booking.setDischargeNotes(request.getDischargeNotes());

                                room.decrementOccupancy();
                                return roomRepository.save(room)
                                        .then(bookingRepository.save(booking))
                                        .flatMap(savedBooking -> mapToDtoWithRoom(savedBooking, room));
                            });
                })
                .doOnSuccess(dto -> {
                    log.info("Patient discharged from room {} with booking ID: {}",
                            dto.getRoomNumber(), dto.getId());
                    notifyPatientDischarged(dto).subscribe();
                });
    }

    @Transactional
    public Mono<RoomBookingDto> cancelBooking(UUID bookingId, String reason) {
        log.info("Cancelling booking: {}", bookingId);

        return bookingRepository.findById(bookingId)
                .switchIfEmpty(Mono.error(new NotFoundException("Room booking", bookingId)))
                .flatMap(booking -> {
                    if (booking.getStatus() == BookingStatus.DISCHARGED) {
                        return Mono.error(new ValidationException("Cannot cancel a discharged booking"));
                    }

                    Mono<Room> roomUpdateMono;
                    if (booking.getStatus() == BookingStatus.CONFIRMED) {
                        roomUpdateMono = roomRepository.findById(booking.getRoomId())
                                .switchIfEmpty(Mono.error(new NotFoundException("Room", booking.getRoomId())))
                                .flatMap(room -> {
                                    room.decrementOccupancy();
                                    return roomRepository.save(room);
                                });
                    } else {
                        roomUpdateMono = Mono.empty();
                    }

                    booking.setStatus(BookingStatus.CANCELLED);
                    booking.setNotes(reason != null ? reason : "Booking cancelled");

                    return roomUpdateMono
                            .then(bookingRepository.save(booking))
                            .flatMap(this::mapToDto);
                })
                .doOnSuccess(dto -> log.info("Booking {} cancelled", bookingId));
    }

    @Transactional(readOnly = true)
    public Mono<RoomBookingDto> getBookingById(UUID id) {
        log.info("Fetching booking by ID: {}", id);

        return bookingRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Room booking", id)))
                .flatMap(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<RoomBookingDto> getBookingsByPatientId(UUID patientId) {
        log.info("Fetching bookings for patient: {}", patientId);
        return bookingRepository.findByPatientId(patientId)
                .flatMap(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Mono<RoomBookingDto> getActiveBookingByPatientId(UUID patientId) {
        log.info("Fetching active booking for patient: {}", patientId);
        return bookingRepository.findActiveBookingByPatientId(patientId)
                .flatMap(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<RoomBookingDto> getBookingsByRoomId(UUID roomId) {
        log.info("Fetching bookings for room: {}", roomId);
        return bookingRepository.findByRoomId(roomId)
                .flatMap(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<RoomBookingDto> getActiveBookingsByRoomId(UUID roomId) {
        log.info("Fetching active bookings for room: {}", roomId);
        return bookingRepository.findActiveBookingsByRoomId(roomId)
                .flatMap(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<RoomBookingDto> getBookingsByStatus(BookingStatus status) {
        log.info("Fetching bookings with status: {}", status);
        return bookingRepository.findByStatus(status)
                .flatMap(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Mono<Long> countActiveBookings() {
        return bookingRepository.countActiveBookings()
                .defaultIfEmpty(0L);
    }

    private Mono<RoomBookingDto> mapToDto(RoomBooking booking) {
        RoomBookingDto dto = modelMapper.map(booking, RoomBookingDto.class);
        dto.setRoomId(booking.getRoomId());
        return roomRepository.findById(booking.getRoomId())
                .map(room -> {
                    dto.setRoomNumber(room.getRoomNumber());
                    return dto;
                })
                .defaultIfEmpty(dto);
    }

    private Mono<RoomBookingDto> mapToDtoWithRoom(RoomBooking booking, Room room) {
        RoomBookingDto dto = modelMapper.map(booking, RoomBookingDto.class);
        dto.setRoomId(booking.getRoomId());
        dto.setRoomNumber(room.getRoomNumber());
        return Mono.just(dto);
    }

    private Mono<Void> notifyPatientAdmitted(RoomBookingDto booking) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/patient-admitted")
                .bodyValue(booking)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify patient admission: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> notifyPatientDischarged(RoomBookingDto booking) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/patient-discharged")
                .bodyValue(booking)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify patient discharge: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
