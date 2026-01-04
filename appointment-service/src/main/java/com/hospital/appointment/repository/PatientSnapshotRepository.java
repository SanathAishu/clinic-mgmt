package com.hospital.appointment.repository;

import com.hospital.appointment.entity.PatientSnapshot;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Patient snapshot repository for R2DBC database operations
 */
@Repository
public interface PatientSnapshotRepository extends R2dbcRepository<PatientSnapshot, UUID> {
}
