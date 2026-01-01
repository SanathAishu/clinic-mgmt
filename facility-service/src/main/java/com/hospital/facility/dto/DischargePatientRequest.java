package com.hospital.facility.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DischargePatientRequest {
    private LocalDate dischargeDate;
    private String dischargeNotes;
}
