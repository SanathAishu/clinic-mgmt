package com.hospital.appointment.service;

import com.hospital.appointment.dto.AppointmentDto;
import com.hospital.appointment.dto.CreateAppointmentRequest;
import com.hospital.appointment.dto.UpdateAppointmentRequest;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.DoctorSnapshot;
import com.hospital.appointment.entity.PatientSnapshot;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.repository.DoctorSnapshotRepository;
import com.hospital.appointment.repository.PatientSnapshotRepository;
import com.hospital.common.enums.AppointmentStatus;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.common.util.DiseaseSpecialtyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientSnapshotRepository patientSnapshotRepository;
    private final DoctorSnapshotRepository doctorSnapshotRepository;
    private final WebClient webClient;

    @Transactional
    public Mono<AppointmentDto> createAppointment(CreateAppointmentRequest request) {
        log.info("Creating appointment for patient {} with doctor {}", request.getPatientId(), request.getDoctorId());

        return Mono.zip(
                patientSnapshotRepository.findById(request.getPatientId())
                        .switchIfEmpty(Mono.error(new NotFoundException("Patient snapshot", request.getPatientId()))),
                doctorSnapshotRepository.findById(request.getDoctorId())
                        .switchIfEmpty(Mono.error(new NotFoundException("Doctor snapshot", request.getDoctorId())))
        ).flatMap(tuple -> {
            PatientSnapshot patient = tuple.getT1();
            DoctorSnapshot doctor = tuple.getT2();

            if (!DiseaseSpecialtyMapper.isSpecialtyMatchingDisease(patient.getDisease(), doctor.getSpecialty())) {
                return Mono.error(new ValidationException(
                        "Doctor specialty " + doctor.getSpecialty() +
                        " does not match patient disease " + patient.getDisease() +
                        ". Expected specialty: " + DiseaseSpecialtyMapper.getSpecialtyForDisease(patient.getDisease())
                ));
            }

            return appointmentRepository.findDoctorAppointmentsAtTime(request.getDoctorId(), request.getAppointmentDate())
                    .collectList()
                    .flatMap(existingAppointments -> {
                        if (!existingAppointments.isEmpty()) {
                            return Mono.error(new ValidationException("Doctor already has an appointment at this time"));
                        }

                        Appointment appointment = Appointment.builder()
                                .patientId(request.getPatientId())
                                .doctorId(request.getDoctorId())
                                .appointmentDate(request.getAppointmentDate())
                                .reason(request.getReason())
                                .status(AppointmentStatus.PENDING)
                                .build();

                        return appointmentRepository.save(appointment)
                                .map(saved -> mapToDto(saved, patient, doctor));
                    });
        }).doOnSuccess(dto -> {
            log.info("Appointment created successfully with ID: {}", dto.getId());
            notifyAppointmentCreated(dto).subscribe();
        });
    }

    @Transactional(readOnly = true)
    public Mono<AppointmentDto> getAppointmentById(UUID id) {
        log.info("Fetching appointment with ID: {}", id);

        return appointmentRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Appointment", id)))
                .flatMap(this::mapToDtoWithSnapshots);
    }

    @Transactional(readOnly = true)
    public Flux<AppointmentDto> getAllAppointments() {
        log.info("Fetching all appointments");

        return appointmentRepository.findAll()
                .flatMap(this::mapToDtoWithSnapshots);
    }

    @Transactional(readOnly = true)
    public Flux<AppointmentDto> getAppointmentsByPatientId(UUID patientId) {
        log.info("Fetching appointments for patient ID: {}", patientId);

        return appointmentRepository.findByPatientId(patientId)
                .flatMap(this::mapToDtoWithSnapshots);
    }

    @Transactional(readOnly = true)
    public Flux<AppointmentDto> getAppointmentsByDoctorId(UUID doctorId) {
        log.info("Fetching appointments for doctor ID: {}", doctorId);

        return appointmentRepository.findByDoctorId(doctorId)
                .flatMap(this::mapToDtoWithSnapshots);
    }

    @Transactional(readOnly = true)
    public Flux<AppointmentDto> getAppointmentsByStatus(AppointmentStatus status) {
        log.info("Fetching appointments with status: {}", status);

        return appointmentRepository.findByStatus(status)
                .flatMap(this::mapToDtoWithSnapshots);
    }

    @Transactional(readOnly = true)
    public Flux<AppointmentDto> getUpcomingAppointments(int hoursAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusHours(hoursAhead);

        return appointmentRepository.findUpcomingAppointments(AppointmentStatus.CONFIRMED, now, endTime)
                .flatMap(this::mapToDtoWithSnapshots);
    }

    @Transactional
    public Mono<AppointmentDto> updateAppointment(UUID id, UpdateAppointmentRequest request) {
        log.info("Updating appointment with ID: {}", id);

        return appointmentRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Appointment", id)))
                .flatMap(appointment -> {
                    if (request.getAppointmentDate() != null) {
                        return appointmentRepository.findDoctorAppointmentsAtTime(appointment.getDoctorId(), request.getAppointmentDate())
                                .collectList()
                                .flatMap(conflicts -> {
                                    if (!conflicts.isEmpty() && !conflicts.get(0).getId().equals(id)) {
                                        return Mono.error(new ValidationException("Doctor already has an appointment at this time"));
                                    }
                                    appointment.setAppointmentDate(request.getAppointmentDate());
                                    return Mono.just(appointment);
                                });
                    }
                    return Mono.just(appointment);
                })
                .flatMap(appointment -> {
                    if (request.getStatus() != null) appointment.setStatus(request.getStatus());
                    if (request.getReason() != null) appointment.setReason(request.getReason());
                    if (request.getNotes() != null) appointment.setNotes(request.getNotes());

                    return appointmentRepository.save(appointment);
                })
                .flatMap(this::mapToDtoWithSnapshots)
                .doOnSuccess(dto -> {
                    log.info("Appointment updated successfully: {}", id);
                    notifyAppointmentUpdated(dto).subscribe();
                    if (dto.getStatus() == AppointmentStatus.CANCELLED) {
                        notifyAppointmentCancelled(dto).subscribe();
                    }
                });
    }

    @Transactional
    public Mono<AppointmentDto> cancelAppointment(UUID id, String reason) {
        log.info("Cancelling appointment with ID: {}", id);

        return appointmentRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Appointment", id)))
                .flatMap(appointment -> {
                    appointment.setStatus(AppointmentStatus.CANCELLED);
                    appointment.setNotes(reason);
                    return appointmentRepository.save(appointment);
                })
                .flatMap(this::mapToDtoWithSnapshots)
                .doOnSuccess(dto -> {
                    log.info("Appointment cancelled: {}", id);
                    notifyAppointmentCancelled(dto).subscribe();
                });
    }

    @Transactional
    public Mono<Void> deleteAppointment(UUID id) {
        log.info("Deleting appointment with ID: {}", id);

        return appointmentRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Appointment", id)))
                .flatMap(appointment -> appointmentRepository.delete(appointment))
                .doOnSuccess(v -> log.info("Appointment deleted successfully: {}", id));
    }

    @Transactional(readOnly = true)
    public Mono<Long> countByStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }

    private Mono<AppointmentDto> mapToDtoWithSnapshots(Appointment appointment) {
        return Mono.zip(
                patientSnapshotRepository.findById(appointment.getPatientId())
                        .defaultIfEmpty(PatientSnapshot.builder().name("Unknown").email("Unknown").build()),
                doctorSnapshotRepository.findById(appointment.getDoctorId())
                        .defaultIfEmpty(DoctorSnapshot.builder().name("Unknown").email("Unknown").build())
        ).map(tuple -> mapToDto(appointment, tuple.getT1(), tuple.getT2()));
    }

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

    private Mono<Void> notifyAppointmentCreated(AppointmentDto appointment) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/appointment-created")
                .bodyValue(appointment)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify appointment creation: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> notifyAppointmentUpdated(AppointmentDto appointment) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/appointment-updated")
                .bodyValue(appointment)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify appointment update: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> notifyAppointmentCancelled(AppointmentDto appointment) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/appointment-cancelled")
                .bodyValue(appointment)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify appointment cancellation: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
