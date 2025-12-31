package com.hospital.auth.dto;

import com.hospital.common.enums.Gender;
import com.hospital.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User DTO for responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String name;
    private String phone;
    private Gender gender;
    private Role role;
    private Boolean active;
    private LocalDateTime createdAt;
}
