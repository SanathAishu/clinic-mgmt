package com.hospital.facility.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    private UUID id;

    @Column
    private String roomNumber;

    @Column
    private RoomType roomType;

    @Column
    private Integer capacity;

    @Column
    private Integer currentOccupancy = 0;

    @Column
    private BigDecimal dailyRate;

    private String floor;

    private String wing;

    private String description;

    @Column
    private Boolean available = true;

    @Column
    private Boolean active = true;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoomBooking> bookings = new ArrayList<>();

    @CreatedDate
    @Column
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public boolean hasAvailableBeds() {
        return currentOccupancy < capacity;
    }

    public void incrementOccupancy() {
        if (currentOccupancy < capacity) {
            currentOccupancy++;
            if (currentOccupancy >= capacity) {
                available = false;
            }
        }
    }

    public void decrementOccupancy() {
        if (currentOccupancy > 0) {
            currentOccupancy--;
            available = true;
        }
    }
}
