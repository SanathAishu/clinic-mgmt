package com.hospital.medicalrecords.repository;

import com.hospital.medicalrecords.entity.MedicalRecord;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends R2dbcRepository<MedicalRecord, UUID> {

    Flux<MedicalRecord> findByPatientIdAndActiveTrue(UUID patientId);

    Flux<MedicalRecord> findByDoctorIdAndActiveTrue(UUID doctorId);

    Flux<MedicalRecord> findByActiveTrue();

    @Query("SELECT * FROM medical_records WHERE patient_id = :patientId AND record_date BETWEEN :startDate AND :endDate AND active = true")
    Flux<MedicalRecord> findByPatientIdAndDateRange(
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT * FROM medical_records WHERE patient_id = :patientId AND LOWER(diagnosis) LIKE LOWER(CONCAT('%', :diagnosis, '%')) AND active = true")
    Flux<MedicalRecord> findByPatientIdAndDiagnosis(
            @Param("patientId") UUID patientId,
            @Param("diagnosis") String diagnosis);

    Mono<Long> countByPatientIdAndActiveTrue(UUID patientId);

    Mono<Long> countByDoctorIdAndActiveTrue(UUID doctorId);
}
