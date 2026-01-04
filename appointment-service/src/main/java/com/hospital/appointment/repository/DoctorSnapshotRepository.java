package com.hospital.appointment.repository;

import com.hospital.appointment.entity.DoctorSnapshot;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Doctor snapshot repository for R2DBC database operations
 */
@Repository
public interface DoctorSnapshotRepository extends R2dbcRepository<DoctorSnapshot, UUID> {
}
