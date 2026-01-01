package com.hospital.facility.repository;

import com.hospital.facility.entity.BookingStatus;
import com.hospital.facility.entity.RoomBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBooking, UUID> {

    List<RoomBooking> findByPatientId(UUID patientId);

    List<RoomBooking> findByRoomId(UUID roomId);

    Optional<RoomBooking> findByPatientIdAndStatus(UUID patientId, BookingStatus status);

    List<RoomBooking> findByStatus(BookingStatus status);

    Page<RoomBooking> findByStatus(BookingStatus status, Pageable pageable);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.patientId = :patientId AND rb.status = 'CONFIRMED'")
    Optional<RoomBooking> findActiveBookingByPatientId(@Param("patientId") UUID patientId);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room.id = :roomId AND rb.status = 'CONFIRMED'")
    List<RoomBooking> findActiveBookingsByRoomId(@Param("roomId") UUID roomId);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.admissionDate BETWEEN :startDate AND :endDate")
    List<RoomBooking> findByAdmissionDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(rb) FROM RoomBooking rb WHERE rb.status = 'CONFIRMED'")
    long countActiveBookings();
}
