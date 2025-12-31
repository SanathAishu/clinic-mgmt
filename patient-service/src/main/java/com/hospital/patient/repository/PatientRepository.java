package com.hospital.patient.repository;

import com.hospital.common.enums.Disease;
import com.hospital.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Patient repository for database operations
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByUserId(UUID userId);

    Optional<Patient> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserId(UUID userId);

    List<Patient> findByDisease(Disease disease);

    Page<Patient> findByDisease(Disease disease, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Patient> searchPatients(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.active = true")
    Page<Patient> findActivePatients(Pageable pageable);

    long countByDisease(Disease disease);
}
