package com.hospital.facility.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

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

    @Column
    private LocalDateTime createdAt;

    @Column
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

    @Override
    public boolean isNew() {
        return isNew;
    }
}
