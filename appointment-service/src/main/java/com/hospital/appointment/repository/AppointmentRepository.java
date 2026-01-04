package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Appointment;
import com.hospital.common.enums.AppointmentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appointment repository for R2DBC database operations
 */
@Repository
public interface AppointmentRepository extends R2dbcRepository<Appointment, UUID> {

    Flux<Appointment> findByPatientId(UUID patientId);

    Flux<Appointment> findByDoctorId(UUID doctorId);

    Flux<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT * FROM appointments WHERE patient_id = :patientId AND status = :status")
    Flux<Appointment> findByPatientIdAndStatus(@Param("patientId") UUID patientId, @Param("status") AppointmentStatus status);

    @Query("SELECT * FROM appointments WHERE doctor_id = :doctorId AND status = :status")
    Flux<Appointment> findByDoctorIdAndStatus(@Param("doctorId") UUID doctorId, @Param("status") AppointmentStatus status);

    @Query("SELECT * FROM appointments WHERE appointment_date BETWEEN :startDate AND :endDate")
    Flux<Appointment> findAppointmentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT * FROM appointments WHERE doctor_id = :doctorId AND appointment_date = :appointmentDate AND status != 'CANCELLED'")
    Flux<Appointment> findDoctorAppointmentsAtTime(@Param("doctorId") UUID doctorId, @Param("appointmentDate") LocalDateTime appointmentDate);

    @Query("SELECT * FROM appointments WHERE status = :status AND appointment_date BETWEEN :startDate AND :endDate")
    Flux<Appointment> findUpcomingAppointments(@Param("status") AppointmentStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    Mono<Long> countByStatus(AppointmentStatus status);

    Mono<Long> countByDoctorIdAndStatus(UUID doctorId, AppointmentStatus status);
}
