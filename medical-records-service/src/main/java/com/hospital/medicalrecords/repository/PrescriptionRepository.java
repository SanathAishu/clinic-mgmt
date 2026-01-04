package com.hospital.medicalrecords.repository;

import com.hospital.medicalrecords.entity.Prescription;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends R2dbcRepository<Prescription, UUID> {

    Flux<Prescription> findByPatientIdAndActiveTrue(UUID patientId);

    Flux<Prescription> findByDoctorIdAndActiveTrue(UUID doctorId);

    Mono<Prescription> findByMedicalRecordId(UUID medicalRecordId);

    @Query("SELECT * FROM prescriptions WHERE patient_id = :patientId AND prescription_date BETWEEN :startDate AND :endDate AND active = true")
    Flux<Prescription> findByPatientIdAndDateRange(
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT * FROM prescriptions WHERE patient_id = :patientId AND refillable = true AND refills_remaining > 0 AND active = true")
    Flux<Prescription> findRefillablePrescriptions(@Param("patientId") UUID patientId);
}
