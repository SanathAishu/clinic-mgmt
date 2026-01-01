package com.hospital.facility.dto;

import com.hospital.facility.entity.BookingStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomBookingDto {
    private UUID id;
    private UUID roomId;
    private String roomNumber;
    private UUID patientId;
    private UUID doctorId;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private BookingStatus status;
    private String admissionReason;
    private String notes;
    private String dischargeNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
