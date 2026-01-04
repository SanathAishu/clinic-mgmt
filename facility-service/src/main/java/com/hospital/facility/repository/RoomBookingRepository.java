package com.hospital.facility.repository;

import com.hospital.facility.entity.BookingStatus;
import com.hospital.facility.entity.RoomBooking;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface RoomBookingRepository extends R2dbcRepository<RoomBooking, UUID> {

    Flux<RoomBooking> findByPatientId(UUID patientId);

    Flux<RoomBooking> findByRoomId(UUID roomId);

    Mono<RoomBooking> findByPatientIdAndStatus(UUID patientId, BookingStatus status);

    Flux<RoomBooking> findByStatus(BookingStatus status);

    @Query("SELECT * FROM room_bookings WHERE patient_id = :patientId AND status = 'CONFIRMED'")
    Mono<RoomBooking> findActiveBookingByPatientId(@Param("patientId") UUID patientId);

    @Query("SELECT * FROM room_bookings WHERE room_id = :roomId AND status = 'CONFIRMED'")
    Flux<RoomBooking> findActiveBookingsByRoomId(@Param("roomId") UUID roomId);

    @Query("SELECT * FROM room_bookings WHERE admission_date BETWEEN :startDate AND :endDate")
    Flux<RoomBooking> findByAdmissionDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(*) FROM room_bookings WHERE status = 'CONFIRMED'")
    Mono<Long> countActiveBookings();
}
