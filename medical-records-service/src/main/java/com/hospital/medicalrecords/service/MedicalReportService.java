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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalReportService {

    private final MedicalReportRepository medicalReportRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public MedicalReportDto createMedicalReport(CreateMedicalReportRequest request) {
        log.info("Creating medical report for medical record: {}", request.getMedicalRecordId());

        MedicalRecord medicalRecord = medicalRecordRepository.findById(request.getMedicalRecordId())
                .orElseThrow(() -> new NotFoundException("Medical record", request.getMedicalRecordId()));

        if (medicalRecord.getMedicalReport() != null) {
            throw new ValidationException("Medical record already has a report");
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

        report = medicalReportRepository.save(report);

        log.info("Created medical report with ID: {}", report.getId());
        return mapToDto(report);
    }

    @Cacheable(value = "medicalReports", key = "#id")
    public MedicalReportDto getMedicalReportById(UUID id) {
        log.info("Fetching medical report by ID: {}", id);

        MedicalReport report = medicalReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical report", id));

        return mapToDto(report);
    }

    public List<MedicalReportDto> getMedicalReportsByPatientId(UUID patientId) {
        log.info("Fetching medical reports for patient: {}", patientId);
        return medicalReportRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MedicalReportDto> getMedicalReportsByDoctorId(UUID doctorId) {
        log.info("Fetching medical reports for doctor: {}", doctorId);
        return medicalReportRepository.findByDoctorIdAndActiveTrue(doctorId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MedicalReportDto> getMedicalReportsByPatientIdAndType(UUID patientId, String reportType) {
        log.info("Fetching medical reports for patient {} with type: {}", patientId, reportType);
        return medicalReportRepository.findByPatientIdAndReportTypeAndActiveTrue(patientId, reportType)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MedicalReportDto> getMedicalReportsByPatientIdAndDateRange(
            UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching medical reports for patient {} between {} and {}", patientId, startDate, endDate);
        return medicalReportRepository.findByPatientIdAndDateRange(patientId, startDate, endDate)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "medicalReports", key = "#id")
    public void deleteMedicalReport(UUID id) {
        log.info("Soft deleting medical report: {}", id);

        MedicalReport report = medicalReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical report", id));

        report.setActive(false);
        medicalReportRepository.save(report);

        log.info("Soft deleted medical report: {}", id);
    }

    private MedicalReportDto mapToDto(MedicalReport report) {
        MedicalReportDto dto = modelMapper.map(report, MedicalReportDto.class);
        dto.setMedicalRecordId(report.getMedicalRecord().getId());
        return dto;
    }
}
