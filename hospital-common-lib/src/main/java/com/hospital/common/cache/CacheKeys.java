package com.hospital.common.cache;

/**
 * Centralized cache key constants to avoid duplication across services.
 * All cache keys are defined here to ensure consistency and ease maintenance.
 *
 * Key format: TENANT_ID:RESOURCE_TYPE:ACTION:IDENTIFIER
 * TTL values are configured in application.properties per cache name.
 *
 * Multi-tenancy: All cache keys include tenantId as the first segment for tenant isolation.
 */
public final class CacheKeys {

    private CacheKeys() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Cache Names (for configuration) ====================
    // Used in @CacheResult(cacheName = "...") annotations

    public static final String CACHE_PATIENTS = "patients";
    public static final String CACHE_DOCTORS = "doctors";
    public static final String CACHE_APPOINTMENTS = "appointments";
    public static final String CACHE_MEDICAL_RECORDS = "medical-records";
    public static final String CACHE_PRESCRIPTIONS = "prescriptions";
    public static final String CACHE_MEDICAL_REPORTS = "medicalReports";
    public static final String CACHE_ROOMS = "rooms";
    public static final String CACHE_ROOM_BOOKINGS = "room-bookings";
    public static final String CACHE_USERS = "users";
    public static final String CACHE_PATIENT_SNAPSHOTS = "patient-snapshots";
    public static final String CACHE_DOCTOR_SNAPSHOTS = "doctor-snapshots";
    public static final String CACHE_PUBLIC_KEYS = "public-keys";

    // ==================== Patient Service Cache Keys ====================

    public static String patientById(String tenantId, String patientId) {
        return String.format("%s:patient:%s", tenantId, patientId);
    }

    public static String patientByEmail(String tenantId, String email) {
        return String.format("%s:patient:email:%s", tenantId, email);
    }

    public static String patientsByGender(String tenantId, String gender) {
        return String.format("%s:patients:gender:%s", tenantId, gender);
    }

    public static String patientsList(String tenantId) {
        return String.format("%s:patients:list", tenantId);
    }

    // ==================== Doctor Service Cache Keys ====================

    public static String doctorById(String tenantId, String doctorId) {
        return String.format("%s:doctor:%s", tenantId, doctorId);
    }

    public static String doctorByEmail(String tenantId, String email) {
        return String.format("%s:doctor:email:%s", tenantId, email);
    }

    public static String doctorsBySpecialty(String tenantId, String specialty) {
        return String.format("%s:doctors:specialty:%s", tenantId, specialty);
    }

    public static String doctorsList(String tenantId) {
        return String.format("%s:doctors:list", tenantId);
    }

    // ==================== Appointment Service Cache Keys ====================

    public static String appointmentById(String tenantId, String appointmentId) {
        return String.format("%s:appointment:%s", tenantId, appointmentId);
    }

    public static String appointmentsByPatient(String tenantId, String patientId) {
        return String.format("%s:appointments:patient:%s", tenantId, patientId);
    }

    public static String appointmentsByDoctor(String tenantId, String doctorId) {
        return String.format("%s:appointments:doctor:%s", tenantId, doctorId);
    }

    public static String appointmentsByStatus(String tenantId, String status) {
        return String.format("%s:appointments:status:%s", tenantId, status);
    }

    // ==================== Appointment Snapshots (Denormalization Cache) ====================

    public static String patientSnapshot(String tenantId, String patientId) {
        return String.format("%s:snapshot:patient:%s", tenantId, patientId);
    }

    public static String doctorSnapshot(String tenantId, String doctorId) {
        return String.format("%s:snapshot:doctor:%s", tenantId, doctorId);
    }

    // ==================== Medical Records Service Cache Keys ====================

    public static String medicalRecordById(String tenantId, String recordId) {
        return String.format("%s:medical-record:%s", tenantId, recordId);
    }

    public static String medicalRecordsByPatient(String tenantId, String patientId) {
        return String.format("%s:medical-records:patient:%s", tenantId, patientId);
    }

    public static String prescriptionById(String tenantId, String prescriptionId) {
        return String.format("%s:prescription:%s", tenantId, prescriptionId);
    }

    public static String prescriptionsByPatient(String tenantId, String patientId) {
        return String.format("%s:prescriptions:patient:%s", tenantId, patientId);
    }

    public static String prescriptionsByDoctor(String tenantId, String doctorId) {
        return String.format("%s:prescriptions:doctor:%s", tenantId, doctorId);
    }

    public static String medicalReportById(String tenantId, String reportId) {
        return String.format("%s:medical-report:%s", tenantId, reportId);
    }

    public static String medicalReportsByPatient(String tenantId, String patientId) {
        return String.format("%s:medical-reports:patient:%s", tenantId, patientId);
    }

    // ==================== Facility Service Cache Keys ====================

    public static String roomById(String tenantId, String roomId) {
        return String.format("%s:room:%s", tenantId, roomId);
    }

    public static String roomsByFloor(String tenantId, String floorNumber) {
        return String.format("%s:rooms:floor:%s", tenantId, floorNumber);
    }

    public static String roomsByType(String tenantId, String roomType) {
        return String.format("%s:rooms:type:%s", tenantId, roomType);
    }

    public static String roomsList(String tenantId) {
        return String.format("%s:rooms:list", tenantId);
    }

    public static String roomBookingById(String tenantId, String bookingId) {
        return String.format("%s:room-booking:%s", tenantId, bookingId);
    }

    public static String roomBookingsByRoom(String tenantId, String roomId) {
        return String.format("%s:room-bookings:room:%s", tenantId, roomId);
    }

    public static String roomBookingsByPatient(String tenantId, String patientId) {
        return String.format("%s:room-bookings:patient:%s", tenantId, patientId);
    }

    // ==================== Auth Service Cache Keys ====================

    public static String userById(String tenantId, String userId) {
        return String.format("%s:user:%s", tenantId, userId);
    }

    public static String userByEmail(String tenantId, String email) {
        return String.format("%s:user:email:%s", tenantId, email);
    }

    public static String usersList(String tenantId) {
        return String.format("%s:users:list", tenantId);
    }

    public static String publicKey(String tenantId, String keyId) {
        return String.format("%s:public-key:%s", tenantId, keyId);
    }

    // ==================== Audit Service Cache Keys ====================

    public static String auditLogsByEntity(String tenantId, String entityType, String entityId) {
        return String.format("%s:audit:logs:%s:%s", tenantId, entityType, entityId);
    }

    public static String auditLogsByUser(String tenantId, String userId) {
        return String.format("%s:audit:logs:user:%s", tenantId, userId);
    }

    public static String auditLogsByAction(String tenantId, String action) {
        return String.format("%s:audit:logs:action:%s", tenantId, action);
    }

    // ==================== Invalidation Patterns ====================
    // Used with @CacheInvalidate for bulk invalidation

    public static String patientPattern(String tenantId) {
        return String.format("%s:patient:*", tenantId);
    }

    public static String doctorPattern(String tenantId) {
        return String.format("%s:doctor:*", tenantId);
    }

    public static String appointmentPattern(String tenantId) {
        return String.format("%s:appointment:*", tenantId);
    }

    public static String medicalRecordPattern(String tenantId) {
        return String.format("%s:medical-record:*", tenantId);
    }

    public static String prescriptionPattern(String tenantId) {
        return String.format("%s:prescription:*", tenantId);
    }

    public static String roomPattern(String tenantId) {
        return String.format("%s:room:*", tenantId);
    }

    public static String roomBookingPattern(String tenantId) {
        return String.format("%s:room-booking:*", tenantId);
    }

    public static String userPattern(String tenantId) {
        return String.format("%s:user:*", tenantId);
    }

    public static String snapshotPattern(String tenantId) {
        return String.format("%s:snapshot:*", tenantId);
    }

    // Invalidate ALL data for a specific tenant (use with caution)
    public static String tenantPattern(String tenantId) {
        return String.format("%s:*", tenantId);
    }
}
