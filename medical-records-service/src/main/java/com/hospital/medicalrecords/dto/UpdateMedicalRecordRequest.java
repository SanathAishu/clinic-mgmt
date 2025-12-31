package com.hospital.medicalrecords.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMedicalRecordRequest {
    private String diagnosis;
    private String symptoms;
    private String treatment;
    private String notes;
    private String bloodPressure;
    private Double temperature;
    private Integer heartRate;
    private Double weight;
    private Double height;
}
