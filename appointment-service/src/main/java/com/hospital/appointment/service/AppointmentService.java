package com.hospital.appointment.service;

import com.hospital.appointment.dto.AppointmentDto;
import com.hospital.appointment.dto.CreateAppointmentRequest;
import com.hospital.appointment.dto.UpdateAppointmentRequest;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.DoctorSnapshot;
import com.hospital.appointment.entity.PatientSnapshot;
import com.hospital.appointment.event.AppointmentEventPublisher;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.repository.DoctorSnapshotRepository;
import com.hospital.appointment.repository.PatientSnapshotRepository;
import com.hospital.common.enums.AppointmentStatus;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.common.util.DiseaseSpecialtyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Appointment service with disease-specialty matching and snapshot integration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientSnapshotRepository patientSnapshotRepository;
    private final DoctorSnapshotRepository doctorSnapshotRepository;
    private final AppointmentEventPublisher eventPublisher;

    /**
     * Create a new appointment with disease-specialty validation
     */
    @Transactional
    public AppointmentDto createAppointment(CreateAppointmentRequest request) {
        log.info("Creating appointment for patient {} with doctor {}", request.getPatientId(), request.getDoctorId());

        // Fetch patient snapshot
        PatientSnapshot patient = patientSnapshotRepository.findById(request.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient snapshot", request.getPatientId()));

        // Fetch doctor snapshot
        DoctorSnapshot doctor = doctorSnapshotRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new NotFoundException("Doctor snapshot", request.getDoctorId()));

        // Validate disease-specialty matching
        if (!DiseaseSpecialtyMapper.isSpecialtyMatchingDisease(patient.getDisease(), doctor.getSpecialty())) {
            throw new ValidationException(
                    "Doctor specialty " + doctor.getSpecialty() +
                    " does not match patient disease " + patient.getDisease() +
                    ". Expected specialty: " + DiseaseSpecialtyMapper.getSpecialtyForDisease(patient.getDisease())
            );
        }

        // Check for doctor availability (no double booking)
        List<Appointment> existingAppointments = appointmentRepository
                .findDoctorAppointmentsAtTime(request.getDoctorId(), request.getAppointmentDate());

        if (!existingAppointments.isEmpty()) {
            throw new ValidationException("Doctor already has an appointment at this time");
        }

        // Create appointment
        Appointment appointment = Appointment.builder()
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .appointmentDate(request.getAppointmentDate())
                .reason(request.getReason())
                .status(AppointmentStatus.PENDING)
                .build();

        appointment = appointmentRepository.save(appointment);

        log.info("Appointment created successfully with ID: {}", appointment.getId());

        // Publish AppointmentCreatedEvent
        eventPublisher.publishAppointmentCreated(appointment, patient, doctor);

        return mapToDto(appointment, patient, doctor);
    }

    /**
     * Get appointment by ID with caching
     */
    @Cacheable(value = "appointments", key = "#id")
    @Transactional(readOnly = true)
    public AppointmentDto getAppointmentById(UUID id) {
        log.info("Fetching appointment with ID: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment", id));

        return mapToDtoWithSnapshots(appointment);
    }

    /**
     * Get all appointments with pagination
     */
    @Transactional(readOnly = true)
    public Page<AppointmentDto> getAllAppointments(Pageable pageable) {
        log.info("Fetching all appointments, page: {}", pageable.getPageNumber());

        return appointmentRepository.findAll(pageable)
                .map(this::mapToDtoWithSnapshots);
    }

    /**
     * Get appointments by patient ID
     */
    @Transactional(readOnly = true)
    public List<AppointmentDto> getAppointmentsByPatientId(UUID patientId) {
        log.info("Fetching appointments for patient ID: {}", patientId);

        return appointmentRepository.findByPatientId(patientId).stream()
                .map(this::mapToDtoWithSnapshots)
                .collect(Collectors.toList());
    }

    /**
     * Get appointments by doctor ID
     */
    @Transactional(readOnly = true)
    public List<AppointmentDto> getAppointmentsByDoctorId(UUID doctorId) {
        log.info("Fetching appointments for doctor ID: {}", doctorId);

        return appointmentRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToDtoWithSnapshots)
                .collect(Collectors.toList());
    }

    /**
     * Get appointments by status
     */
    @Transactional(readOnly = true)
    public List<AppointmentDto> getAppointmentsByStatus(AppointmentStatus status) {
        log.info("Fetching appointments with status: {}", status);

        return appointmentRepository.findByStatus(status).stream()
                .map(this::mapToDtoWithSnapshots)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming appointments (for reminders)
     */
    @Transactional(readOnly = true)
    public List<AppointmentDto> getUpcomingAppointments(int hoursAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusHours(hoursAhead);

        return appointmentRepository.findUpcomingAppointments(AppointmentStatus.CONFIRMED, now, endTime).stream()
                .map(this::mapToDtoWithSnapshots)
                .collect(Collectors.toList());
    }

    /**
     * Update appointment with cache update
     */
    @CachePut(value = "appointments", key = "#id")
    @Transactional
    public AppointmentDto updateAppointment(UUID id, UpdateAppointmentRequest request) {
        log.info("Updating appointment with ID: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment", id));

        // Update fields
        if (request.getAppointmentDate() != null) {
            // Check for doctor availability at new time
            List<Appointment> conflicts = appointmentRepository
                    .findDoctorAppointmentsAtTime(appointment.getDoctorId(), request.getAppointmentDate());

            if (!conflicts.isEmpty() && !conflicts.get(0).getId().equals(id)) {
                throw new ValidationException("Doctor already has an appointment at this time");
            }
            appointment.setAppointmentDate(request.getAppointmentDate());
        }

        if (request.getStatus() != null) appointment.setStatus(request.getStatus());
        if (request.getReason() != null) appointment.setReason(request.getReason());
        if (request.getNotes() != null) appointment.setNotes(request.getNotes());

        appointment = appointmentRepository.save(appointment);

        log.info("Appointment updated successfully: {}", id);

        // Publish event if status changed to CANCELLED
        if (request.getStatus() == AppointmentStatus.CANCELLED) {
            eventPublisher.publishAppointmentCancelled(appointment);
        }

        return mapToDtoWithSnapshots(appointment);
    }

    /**
     * Cancel appointment
     */
    @CachePut(value = "appointments", key = "#id")
    @Transactional
    public AppointmentDto cancelAppointment(UUID id, String reason) {
        log.info("Cancelling appointment with ID: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment", id));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setNotes(reason);
        appointment = appointmentRepository.save(appointment);

        log.info("Appointment cancelled: {}", id);

        // Publish AppointmentCancelledEvent
        eventPublisher.publishAppointmentCancelled(appointment);

        return mapToDtoWithSnapshots(appointment);
    }

    /**
     * Delete appointment with cache eviction
     */
    @CacheEvict(value = "appointments", key = "#id")
    @Transactional
    public void deleteAppointment(UUID id) {
        log.info("Deleting appointment with ID: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment", id));

        appointmentRepository.delete(appointment);

        log.info("Appointment deleted successfully: {}", id);
    }

    /**
     * Get appointment count by status
     */
    @Transactional(readOnly = true)
    public long countByStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }

    /**
     * Map Appointment to DTO with snapshots
     */
    private AppointmentDto mapToDtoWithSnapshots(Appointment appointment) {
        PatientSnapshot patient = patientSnapshotRepository.findById(appointment.getPatientId())
                .orElse(null);
        DoctorSnapshot doctor = doctorSnapshotRepository.findById(appointment.getDoctorId())
                .orElse(null);

        return mapToDto(appointment, patient, doctor);
    }

    /**
     * Map Appointment to DTO with provided snapshots
     */
    private AppointmentDto mapToDto(Appointment appointment, PatientSnapshot patient, DoctorSnapshot doctor) {
        return AppointmentDto.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatientId())
                .patientName(patient != null ? patient.getName() : "Unknown")
                .patientEmail(patient != null ? patient.getEmail() : "Unknown")
                .patientDisease(patient != null ? patient.getDisease() : null)
                .doctorId(appointment.getDoctorId())
                .doctorName(doctor != null ? doctor.getName() : "Unknown")
                .doctorEmail(doctor != null ? doctor.getEmail() : "Unknown")
                .doctorSpecialty(doctor != null ? doctor.getSpecialty() : null)
                .appointmentDate(appointment.getAppointmentDate())
                .status(appointment.getStatus())
                .reason(appointment.getReason())
                .notes(appointment.getNotes())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
