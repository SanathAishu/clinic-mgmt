package com.hospital.facility.dto;

import com.hospital.facility.entity.RoomType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoomRequest {
    private RoomType roomType;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity cannot exceed 20")
    private Integer capacity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Daily rate must be positive")
    private BigDecimal dailyRate;

    private String floor;
    private String wing;
    private String description;
    private Boolean available;
}
