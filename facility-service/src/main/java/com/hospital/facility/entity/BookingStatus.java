package com.hospital.facility.entity;

public enum BookingStatus {
    PENDING,        // Saga started, waiting for patient service confirmation
    CONFIRMED,      // Patient admitted, booking active
    DISCHARGED,     // Patient discharged
    CANCELLED,      // Booking cancelled (saga compensation)
    FAILED          // Saga failed
}
