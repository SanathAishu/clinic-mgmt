package com.hospital.doctor.repository;

import com.hospital.common.enums.Specialty;
import com.hospital.doctor.entity.Doctor;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Doctor repository for R2DBC database operations
 */
@Repository
public interface DoctorRepository extends R2dbcRepository<Doctor, UUID> {

    Mono<Doctor> findByUserId(UUID userId);

    Mono<Doctor> findByEmail(String email);

    Mono<Doctor> findByLicenseNumber(String licenseNumber);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByUserId(UUID userId);

    Mono<Boolean> existsByLicenseNumber(String licenseNumber);

    Flux<Doctor> findBySpecialty(Specialty specialty);

    @Query("SELECT * FROM doctors WHERE specialty = :specialty AND available = true AND active = true")
    Flux<Doctor> findAvailableDoctorsBySpecialty(@Param("specialty") Specialty specialty);

    @Query("SELECT * FROM doctors WHERE LOWER(name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Flux<Doctor> searchDoctors(@Param("search") String search);

    @Query("SELECT * FROM doctors WHERE active = true")
    Flux<Doctor> findActiveDoctors();

    @Query("SELECT * FROM doctors WHERE available = true AND active = true")
    Flux<Doctor> findAvailableDoctors();

    Mono<Long> countBySpecialty(Specialty specialty);

    @Query("SELECT COUNT(*) FROM doctors WHERE specialty = :specialty AND available = true AND active = true")
    Mono<Long> countAvailableBySpecialty(@Param("specialty") Specialty specialty);
}
