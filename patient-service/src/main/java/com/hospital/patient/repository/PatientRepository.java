package com.hospital.patient.repository;

import com.hospital.common.enums.Disease;
import com.hospital.patient.entity.Patient;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Patient repository for R2DBC database operations
 */
@Repository
public interface PatientRepository extends R2dbcRepository<Patient, UUID> {

    Mono<Patient> findByUserId(UUID userId);

    Mono<Patient> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByUserId(UUID userId);

    Flux<Patient> findByDisease(Disease disease);

    @Query("SELECT * FROM patients p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Flux<Patient> searchPatients(@Param("search") String search);

    @Query("SELECT * FROM patients WHERE active = true")
    Flux<Patient> findActivePatients();

    Mono<Long> countByDisease(Disease disease);
}
