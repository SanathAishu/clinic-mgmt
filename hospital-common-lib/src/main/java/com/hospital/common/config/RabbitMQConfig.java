package com.hospital.common.config;

/**
 * Centralized RabbitMQ configuration constants.
 * Defines all exchanges, queues, and routing keys for event-driven communication.
 *
 * Architecture:
 * - Topic Exchange: hospital.events.topic - for broadcast notifications (pattern matching)
 * - Direct Exchange: hospital.events.direct - for specific service-to-service events
 * - Direct Exchange: hospital.events.saga - for saga pattern (distributed transactions)
 */
public final class RabbitMQConfig {

    private RabbitMQConfig() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Exchanges ====================

    public static final String TOPIC_EXCHANGE = "hospital.events.topic";
    public static final String DIRECT_EXCHANGE = "hospital.events.direct";
    public static final String SAGA_EXCHANGE = "hospital.events.saga";

    // ==================== Patient Service Queues & Routes ====================

    // Patient notifications (fan-out to multiple listeners)
    public static final String PATIENT_NOTIFICATIONS_QUEUE = "patient.notifications";
    public static final String PATIENT_UPDATES_QUEUE = "patient.updates";  // For snapshots
    public static final String PATIENT_CREATED_ROUTING_KEY = "patient.created";
    public static final String PATIENT_UPDATED_ROUTING_KEY = "patient.updated";
    public static final String PATIENT_DELETED_ROUTING_KEY = "patient.deleted";

    // ==================== Doctor Service Queues & Routes ====================

    // Doctor notifications (fan-out to multiple listeners)
    public static final String DOCTOR_NOTIFICATIONS_QUEUE = "doctor.notifications";
    public static final String DOCTOR_UPDATES_QUEUE = "doctor.updates";  // For snapshots
    public static final String DOCTOR_CREATED_ROUTING_KEY = "doctor.created";
    public static final String DOCTOR_UPDATED_ROUTING_KEY = "doctor.updated";
    public static final String DOCTOR_DELETED_ROUTING_KEY = "doctor.deleted";

    // ==================== Appointment Service Queues & Routes ====================

    public static final String APPOINTMENT_NOTIFICATIONS_QUEUE = "appointment.notifications";
    public static final String APPOINTMENT_CREATED_ROUTING_KEY = "appointment.created";
    public static final String APPOINTMENT_CANCELLED_ROUTING_KEY = "appointment.cancelled";
    public static final String APPOINTMENT_COMPLETED_ROUTING_KEY = "appointment.completed";

    // ==================== Medical Records Service Queues & Routes ====================

    public static final String MEDICAL_RECORD_NOTIFICATIONS_QUEUE = "medical.notifications";
    public static final String MEDICAL_RECORD_CREATED_ROUTING_KEY = "medical-record.created";

    public static final String PRESCRIPTION_NOTIFICATIONS_QUEUE = "prescription.notifications";
    public static final String PRESCRIPTION_CREATED_ROUTING_KEY = "prescription.created";

    public static final String MEDICAL_REPORT_NOTIFICATIONS_QUEUE = "medical-report.notifications";
    public static final String MEDICAL_REPORT_CREATED_ROUTING_KEY = "medical-report.created";

    // ==================== Facility Service Queues & Routes ====================

    public static final String FACILITY_NOTIFICATIONS_QUEUE = "facility.notifications";
    public static final String ROOM_CREATED_ROUTING_KEY = "room.created";

    // Admission Saga (distributed transaction)
    public static final String ADMISSION_REQUEST_QUEUE = "patient.admission.request";
    public static final String ADMISSION_REQUEST_ROUTING_KEY = "patient.admission.request";

    public static final String ADMISSION_SUCCESS_QUEUE = "admission.success";
    public static final String ADMISSION_SUCCESS_ROUTING_KEY = "admission.success";

    public static final String ADMISSION_FAILED_QUEUE = "admission.failed";
    public static final String ADMISSION_FAILED_ROUTING_KEY = "admission.failed";

    // ==================== Notification Service Queues ====================
    // Consumes from multiple queues to send notifications

    public static final String NOTIFICATION_QUEUE_APPOINTMENT = "notification.appointments";
    public static final String NOTIFICATION_QUEUE_PATIENT = "notification.patients";
    public static final String NOTIFICATION_QUEUE_MEDICAL = "notification.medical";
    public static final String NOTIFICATION_QUEUE_FACILITY = "notification.facility";

    // ==================== Audit Service Queues ====================

    public static final String AUDIT_LOG_QUEUE = "audit.logs";

    // ==================== Cache Invalidation ====================

    public static final String CACHE_INVALIDATION_QUEUE = "cache.invalidation";
    public static final String CACHE_INVALIDATION_ROUTING_KEY = "cache.invalidate";

    // ==================== Queue Binding Configuration ====================

    /**
     * Returns the queue configuration for a given queue name.
     * Used when configuring SmallRye Messaging incoming/outgoing channels.
     *
     * Example usage in application.properties:
     * mp.messaging.incoming.patient-updates.queue.name=patient.updates
     * mp.messaging.incoming.patient-updates.exchange.name=hospital.events.direct
     * mp.messaging.incoming.patient-updates.routing-keys=patient.created,patient.updated
     */
    public static final class QueueConfig {

        public static class PatientQueues {
            public static final String NOTIFICATIONS = PATIENT_NOTIFICATIONS_QUEUE;
            public static final String UPDATES = PATIENT_UPDATES_QUEUE;
        }

        public static class DoctorQueues {
            public static final String NOTIFICATIONS = DOCTOR_NOTIFICATIONS_QUEUE;
            public static final String UPDATES = DOCTOR_UPDATES_QUEUE;
        }

        public static class AppointmentQueues {
            public static final String NOTIFICATIONS = APPOINTMENT_NOTIFICATIONS_QUEUE;
        }

        public static class MedicalQueues {
            public static final String RECORDS = MEDICAL_RECORD_NOTIFICATIONS_QUEUE;
            public static final String PRESCRIPTIONS = PRESCRIPTION_NOTIFICATIONS_QUEUE;
            public static final String REPORTS = MEDICAL_REPORT_NOTIFICATIONS_QUEUE;
        }

        public static class FacilityQueues {
            public static final String NOTIFICATIONS = FACILITY_NOTIFICATIONS_QUEUE;
            public static final String ADMISSION_REQUEST = ADMISSION_REQUEST_QUEUE;
            public static final String ADMISSION_SUCCESS = ADMISSION_SUCCESS_QUEUE;
            public static final String ADMISSION_FAILED = ADMISSION_FAILED_QUEUE;
        }

        public static class AuditQueues {
            public static final String LOGS = AUDIT_LOG_QUEUE;
        }

        public static class NotificationQueues {
            public static final String APPOINTMENTS = NOTIFICATION_QUEUE_APPOINTMENT;
            public static final String PATIENTS = NOTIFICATION_QUEUE_PATIENT;
            public static final String MEDICAL = NOTIFICATION_QUEUE_MEDICAL;
            public static final String FACILITY = NOTIFICATION_QUEUE_FACILITY;
        }
    }

    // ==================== SmallRye Messaging Configuration ====================

    /**
     * Configuration template for outgoing (publishing) channels:
     *
     * # Patient Service Publishing PatientCreatedEvent
     * mp.messaging.outgoing.patient-events.connector=smallrye-rabbitmq
     * mp.messaging.outgoing.patient-events.exchange.name=hospital.events.direct
     * mp.messaging.outgoing.patient-events.routing-key=patient.created
     * mp.messaging.outgoing.patient-events.exchange.type=direct
     * mp.messaging.outgoing.patient-events.exchange.durable=true
     */

    /**
     * Configuration template for incoming (listening) channels:
     *
     * # Appointment Service Listening for Patient Updates (Snapshot Pattern)
     * mp.messaging.incoming.patient-updates.connector=smallrye-rabbitmq
     * mp.messaging.incoming.patient-updates.queue.name=patient.updates
     * mp.messaging.incoming.patient-updates.exchange.name=hospital.events.direct
     * mp.messaging.incoming.patient-updates.routing-keys=patient.created,patient.updated
     * mp.messaging.incoming.patient-updates.queue.durable=true
     * mp.messaging.incoming.patient-updates.queue.auto-bind=true
     */

    // ==================== Helper Methods ====================

    public static String[] getTopicRoutingPatterns(String topic) {
        return new String[]{topic + ".*"};
    }

    public static String buildTopicPattern(String... parts) {
        return String.join(".", parts) + ".*";
    }
}
