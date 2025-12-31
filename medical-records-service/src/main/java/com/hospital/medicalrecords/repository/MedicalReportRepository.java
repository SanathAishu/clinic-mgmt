package com.hospital.medicalrecords.repository;

import com.hospital.medicalrecords.entity.MedicalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalReportRepository extends JpaRepository<MedicalReport, UUID> {

    List<MedicalReport> findByPatientIdAndActiveTrue(UUID patientId);

    List<MedicalReport> findByDoctorIdAndActiveTrue(UUID doctorId);

    Optional<MedicalReport> findByMedicalRecordId(UUID medicalRecordId);

    List<MedicalReport> findByPatientIdAndReportTypeAndActiveTrue(UUID patientId, String reportType);

    @Query("SELECT mr FROM MedicalReport mr WHERE mr.patientId = :patientId AND mr.reportDate BETWEEN :startDate AND :endDate AND mr.active = true")
    List<MedicalReport> findByPatientIdAndDateRange(
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
