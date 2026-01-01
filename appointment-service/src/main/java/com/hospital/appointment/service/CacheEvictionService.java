package com.hospital.appointment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Service for handling cache eviction across the appointment service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEvictionService {

    private final CacheManager cacheManager;

    /**
     * Evict all entries from a specific cache
     */
    public void evictAllFromCache(String cacheName) {
        log.info("Evicting all entries from cache: {}", cacheName);
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Successfully cleared cache: {}", cacheName);
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }

    /**
     * Evict a specific entry from a cache
     */
    public void evictFromCache(String cacheName, Object key) {
        log.info("Evicting key {} from cache: {}", key, cacheName);
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.info("Successfully evicted key {} from cache: {}", key, cacheName);
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }

    /**
     * Evict appointments cache when patient data changes
     * This invalidates cached appointments that might contain stale patient data
     */
    public void evictAppointmentsCacheForPatient(UUID patientId) {
        log.info("Evicting appointment caches for patient: {}", patientId);
        // Evict patient-related caches
        evictFromCache("patient-snapshots", patientId);
        // Clear all appointments cache as they might contain this patient's data
        // In a production system, you might want more granular cache invalidation
        evictAllFromCache("appointments");
    }

    /**
     * Evict appointments cache when doctor data changes
     * This invalidates cached appointments that might contain stale doctor data
     */
    public void evictAppointmentsCacheForDoctor(UUID doctorId) {
        log.info("Evicting appointment caches for doctor: {}", doctorId);
        // Evict doctor-related caches
        evictFromCache("doctor-snapshots", doctorId);
        // Clear all appointments cache as they might contain this doctor's data
        evictAllFromCache("appointments");
    }

    /**
     * Evict all appointment-related caches
     */
    public void evictAllAppointmentCaches() {
        log.info("Evicting all appointment-related caches");
        evictAllFromCache("appointments");
        evictAllFromCache("patient-snapshots");
        evictAllFromCache("doctor-snapshots");
    }
}
