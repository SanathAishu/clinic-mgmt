package com.hospital.medicalrecords.repository;

import com.hospital.medicalrecords.entity.MedicalReport;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface MedicalReportRepository extends R2dbcRepository<MedicalReport, UUID> {

    Flux<MedicalReport> findByPatientIdAndActiveTrue(UUID patientId);

    Flux<MedicalReport> findByDoctorIdAndActiveTrue(UUID doctorId);

    Mono<MedicalReport> findByMedicalRecordId(UUID medicalRecordId);

    Flux<MedicalReport> findByPatientIdAndReportTypeAndActiveTrue(UUID patientId, String reportType);

    @Query("SELECT * FROM medical_reports WHERE patient_id = :patientId AND report_date BETWEEN :startDate AND :endDate AND active = true")
    Flux<MedicalReport> findByPatientIdAndDateRange(
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
