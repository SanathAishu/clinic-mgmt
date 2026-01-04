package com.hospital.facility.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "room_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomBooking {

    @Id
    private UUID id;

    @Column
    private UUID roomId;

    @Column
    private UUID patientId;

    @Column
    private UUID doctorId;

    @Column
    private LocalDate admissionDate;

    private LocalDate dischargeDate;

    @Column
    private BookingStatus status;

    @Column
    private String admissionReason;

    @Column
    private String notes;

    @Column
    private String dischargeNotes;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
