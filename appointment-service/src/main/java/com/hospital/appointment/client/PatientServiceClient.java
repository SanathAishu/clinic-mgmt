package com.hospital.appointment.client;

import com.hospital.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

/**
 * Feign client for Patient Service
 */
@FeignClient(name = "patient-service")
public interface PatientServiceClient {

    @GetMapping("/api/patients/{id}")
    ApiResponse<Map<String, Object>> getPatientById(@PathVariable UUID id);
}
