package com.hospital.appointment.repository;

import com.hospital.appointment.entity.DoctorSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Doctor snapshot repository
 */
@Repository
public interface DoctorSnapshotRepository extends JpaRepository<DoctorSnapshot, UUID> {
}
