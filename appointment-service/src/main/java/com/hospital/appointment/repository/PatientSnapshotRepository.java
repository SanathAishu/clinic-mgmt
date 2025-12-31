package com.hospital.appointment.repository;

import com.hospital.appointment.entity.PatientSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Patient snapshot repository
 */
@Repository
public interface PatientSnapshotRepository extends JpaRepository<PatientSnapshot, UUID> {
}
