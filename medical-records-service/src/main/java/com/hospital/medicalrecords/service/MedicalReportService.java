package com.hospital.medicalrecords.service;

import com.hospital.common.exception.NotFoundException;
import com.hospital.common.exception.ValidationException;
import com.hospital.medicalrecords.dto.*;
import com.hospital.medicalrecords.entity.MedicalRecord;
import com.hospital.medicalrecords.entity.MedicalReport;
import com.hospital.medicalrecords.repository.MedicalRecordRepository;
import com.hospital.medicalrecords.repository.MedicalReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalReportService {

    private final MedicalReportRepository medicalReportRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ModelMapper modelMapper;
    private final WebClient webClient;

    @Transactional
    public Mono<MedicalReportDto> createMedicalReport(CreateMedicalReportRequest request) {
        log.info("Creating medical report for medical record: {}", request.getMedicalRecordId());

        return medicalRecordRepository.findById(request.getMedicalRecordId())
                .switchIfEmpty(Mono.error(new NotFoundException("Medical record", request.getMedicalRecordId())))
                .flatMap(medicalRecord -> {
                    if (medicalRecord.getMedicalReport() != null) {
                        return Mono.error(new ValidationException("Medical record already has a report"));
                    }

                    MedicalReport report = MedicalReport.builder()
                            .medicalRecord(medicalRecord)
                            .patientId(medicalRecord.getPatientId())
                            .doctorId(medicalRecord.getDoctorId())
                            .reportType(request.getReportType())
                            .reportDate(request.getReportDate())
                            .findings(request.getFindings())
                            .conclusion(request.getConclusion())
                            .recommendations(request.getRecommendations())
                            .labName(request.getLabName())
                            .technicianName(request.getTechnicianName())
                            .active(true)
                            .build();

                    return medicalReportRepository.save(report);
                })
                .map(this::mapToDto)
                .doOnSuccess(dto -> {
                    log.info("Created medical report with ID: {}", dto.getId());
                    notifyMedicalReportCreated(dto).subscribe();
                });
    }

    @Transactional(readOnly = true)
    public Mono<MedicalReportDto> getMedicalReportById(UUID id) {
        log.info("Fetching medical report by ID: {}", id);

        return medicalReportRepository.findById(id)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new NotFoundException("Medical report", id)));
    }

    @Transactional(readOnly = true)
    public Flux<MedicalReportDto> getMedicalReportsByPatientId(UUID patientId) {
        log.info("Fetching medical reports for patient: {}", patientId);
        return medicalReportRepository.findByPatientIdAndActiveTrue(patientId)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<MedicalReportDto> getMedicalReportsByDoctorId(UUID doctorId) {
        log.info("Fetching medical reports for doctor: {}", doctorId);
        return medicalReportRepository.findByDoctorIdAndActiveTrue(doctorId)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<MedicalReportDto> getMedicalReportsByPatientIdAndType(UUID patientId, String reportType) {
        log.info("Fetching medical reports for patient {} with type: {}", patientId, reportType);
        return medicalReportRepository.findByPatientIdAndReportTypeAndActiveTrue(patientId, reportType)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Flux<MedicalReportDto> getMedicalReportsByPatientIdAndDateRange(
            UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching medical reports for patient {} between {} and {}", patientId, startDate, endDate);
        return medicalReportRepository.findByPatientIdAndDateRange(patientId, startDate, endDate)
                .map(this::mapToDto);
    }

    @Transactional
    public Mono<Void> deleteMedicalReport(UUID id) {
        log.info("Soft deleting medical report: {}", id);

        return medicalReportRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Medical report", id)))
                .flatMap(report -> {
                    report.setActive(false);
                    return medicalReportRepository.save(report);
                })
                .doOnSuccess(report -> log.info("Soft deleted medical report: {}", id))
                .then();
    }

    private MedicalReportDto mapToDto(MedicalReport report) {
        MedicalReportDto dto = modelMapper.map(report, MedicalReportDto.class);
        if (report.getMedicalRecordId() != null) {
            dto.setMedicalRecordId(report.getMedicalRecordId());
        } else if (report.getMedicalRecord() != null) {
            dto.setMedicalRecordId(report.getMedicalRecord().getId());
        }
        return dto;
    }

    private Mono<Void> notifyMedicalReportCreated(MedicalReportDto report) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/medical-report-created")
                .bodyValue(report)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to notify medical report creation: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
