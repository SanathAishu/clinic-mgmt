package com.hospital.doctor.repository;

import com.hospital.common.enums.Specialty;
import com.hospital.doctor.entity.Doctor;
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
 * Doctor repository for database operations
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    Optional<Doctor> findByUserId(UUID userId);

    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByLicenseNumber(String licenseNumber);

    boolean existsByEmail(String email);

    boolean existsByUserId(UUID userId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Doctor> findBySpecialty(Specialty specialty);

    Page<Doctor> findBySpecialty(Specialty specialty, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.specialty = :specialty AND d.available = true AND d.active = true")
    List<Doctor> findAvailableDoctorsBySpecialty(@Param("specialty") Specialty specialty);

    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Doctor> searchDoctors(@Param("search") String search, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.active = true")
    Page<Doctor> findActiveDoctors(Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.available = true AND d.active = true")
    Page<Doctor> findAvailableDoctors(Pageable pageable);

    long countBySpecialty(Specialty specialty);

    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.specialty = :specialty AND d.available = true AND d.active = true")
    long countAvailableBySpecialty(@Param("specialty") Specialty specialty);
}
