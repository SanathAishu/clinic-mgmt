package com.hospital.facility.dto;

import com.hospital.facility.entity.RoomType;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private String roomNumber;
    private RoomType roomType;
    private Integer capacity;
    private Integer currentOccupancy;
    private Integer availableBeds;
    private BigDecimal dailyRate;
    private String floor;
    private String wing;
    private String description;
    private Boolean available;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
