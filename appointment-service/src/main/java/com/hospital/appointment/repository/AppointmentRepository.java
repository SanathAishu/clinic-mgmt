package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Appointment;
import com.hospital.common.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Appointment repository
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientId(UUID patientId);

    Page<Appointment> findByPatientId(UUID patientId, Pageable pageable);

    List<Appointment> findByDoctorId(UUID doctorId);

    Page<Appointment> findByDoctorId(UUID doctorId, Pageable pageable);

    List<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.status = :status")
    List<Appointment> findByPatientIdAndStatus(@Param("patientId") UUID patientId, @Param("status") AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.status = :status")
    List<Appointment> findByDoctorIdAndStatus(@Param("doctorId") UUID doctorId, @Param("status") AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate BETWEEN :startDate AND :endDate")
    List<Appointment> findAppointmentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate = :appointmentDate AND a.status != 'CANCELLED'")
    List<Appointment> findDoctorAppointmentsAtTime(@Param("doctorId") UUID doctorId, @Param("appointmentDate") LocalDateTime appointmentDate);

    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.appointmentDate BETWEEN :startDate AND :endDate")
    List<Appointment> findUpcomingAppointments(@Param("status") AppointmentStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    long countByStatus(AppointmentStatus status);

    long countByDoctorIdAndStatus(UUID doctorId, AppointmentStatus status);
}
