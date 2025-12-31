package com.hospital.medicalrecords.repository;

import com.hospital.medicalrecords.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {

    List<MedicalRecord> findByPatientIdAndActiveTrue(UUID patientId);

    List<MedicalRecord> findByDoctorIdAndActiveTrue(UUID doctorId);

    Page<MedicalRecord> findByActiveTrue(Pageable pageable);

    @Query("SELECT mr FROM MedicalRecord mr WHERE mr.patientId = :patientId AND mr.recordDate BETWEEN :startDate AND :endDate AND mr.active = true")
    List<MedicalRecord> findByPatientIdAndDateRange(
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT mr FROM MedicalRecord mr WHERE mr.patientId = :patientId AND LOWER(mr.diagnosis) LIKE LOWER(CONCAT('%', :diagnosis, '%')) AND mr.active = true")
    List<MedicalRecord> findByPatientIdAndDiagnosis(
            @Param("patientId") UUID patientId,
            @Param("diagnosis") String diagnosis);

    long countByPatientIdAndActiveTrue(UUID patientId);

    long countByDoctorIdAndActiveTrue(UUID doctorId);
}
