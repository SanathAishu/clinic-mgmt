package com.hospital.medicalrecords.repository;

import com.hospital.medicalrecords.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    List<Prescription> findByPatientIdAndActiveTrue(UUID patientId);

    List<Prescription> findByDoctorIdAndActiveTrue(UUID doctorId);

    Optional<Prescription> findByMedicalRecordId(UUID medicalRecordId);

    @Query("SELECT p FROM Prescription p WHERE p.patientId = :patientId AND p.prescriptionDate BETWEEN :startDate AND :endDate AND p.active = true")
    List<Prescription> findByPatientIdAndDateRange(
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Prescription p WHERE p.patientId = :patientId AND p.refillable = true AND p.refillsRemaining > 0 AND p.active = true")
    List<Prescription> findRefillablePrescriptions(@Param("patientId") UUID patientId);
}
