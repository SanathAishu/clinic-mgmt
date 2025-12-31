package com.hospital.appointment.client;

import com.hospital.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

/**
 * Feign client for Doctor Service
 */
@FeignClient(name = "doctor-service")
public interface DoctorServiceClient {

    @GetMapping("/api/doctors/{id}")
    ApiResponse<Map<String, Object>> getDoctorById(@PathVariable UUID id);
}
