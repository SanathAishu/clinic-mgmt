package com.hospital.common.config;

/**
 * Consul Service Discovery Configuration Constants.
 *
 * Usage in application.properties:
 *
 * # Consul Configuration
 * quarkus.consul-config.enabled=true
 * quarkus.consul-config.agent.host-port=localhost:8500
 *
 * # Service Registration
 * quarkus.application.name=patient-service
 * quarkus.consul-config.properties-value-keys=config/hospital/${quarkus.application.name}
 *
 * # Health Check
 * quarkus.consul-config.agent.health-check.enabled=true
 * quarkus.consul-config.agent.health-check.interval=10s
 *
 * Service Discovery with Stork (client-side load balancing):
 *
 * # In application.properties
 * quarkus.stork.patient-service.service-discovery.type=consul
 * quarkus.stork.patient-service.service-discovery.consul-host=localhost
 * quarkus.stork.patient-service.service-discovery.consul-port=8500
 *
 * # In Java code
 * @Inject
 * @RestClient
 * PatientServiceClient patientClient;  // Stork will load balance
 */
public final class ConsulConfig {

    private ConsulConfig() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Service Names (for discovery) ====================

    public static final String AUTH_SERVICE = "auth-service";
    public static final String PATIENT_SERVICE = "patient-service";
    public static final String DOCTOR_SERVICE = "doctor-service";
    public static final String APPOINTMENT_SERVICE = "appointment-service";
    public static final String MEDICAL_RECORDS_SERVICE = "medical-records-service";
    public static final String FACILITY_SERVICE = "facility-service";
    public static final String NOTIFICATION_SERVICE = "notification-service";
    public static final String AUDIT_SERVICE = "audit-service";
    public static final String API_GATEWAY = "api-gateway";

    // ==================== Default Consul Configuration ====================

    public static final String DEFAULT_CONSUL_HOST = "localhost";
    public static final int DEFAULT_CONSUL_PORT = 8500;
    public static final String DEFAULT_CONSUL_HEALTH_CHECK_INTERVAL = "10s";
    public static final String DEFAULT_CONSUL_HEALTH_CHECK_TIMEOUT = "5s";

    // ==================== Service Discovery Endpoints ====================

    /**
     * Get Consul agent URL
     */
    public static String getConsulAgentUrl(String host, int port) {
        return String.format("http://%s:%d", host, port);
    }

    /**
     * Get service discovery URL for a given service
     */
    public static String getServiceDiscoveryUrl(String serviceName) {
        return String.format("stork://%s", serviceName);
    }

    // ==================== Configuration Template ====================

    /**
     * Template for service registration in Consul.
     *
     * Add to each service's application.properties:
     *
     * # Consul Service Discovery
     * quarkus.consul-config.enabled=true
     * quarkus.consul-config.agent.host-port=${CONSUL_HOST:localhost}:${CONSUL_PORT:8500}
     * quarkus.application.name=patient-service
     *
     * # Service Registration
     * quarkus.consul-config.agent.health-check.enabled=true
     * quarkus.consul-config.agent.health-check.interval=10s
     * quarkus.consul-config.agent.health-check.timeout=5s
     *
     * # Stork Client-Side Load Balancing (for calling other services)
     * quarkus.stork.patient-service.service-discovery.type=consul
     * quarkus.stork.patient-service.service-discovery.consul-host=${CONSUL_HOST:localhost}
     * quarkus.stork.patient-service.service-discovery.consul-port=${CONSUL_PORT:8500}
     *
     * quarkus.stork.doctor-service.service-discovery.type=consul
     * quarkus.stork.doctor-service.service-discovery.consul-host=${CONSUL_HOST:localhost}
     * quarkus.stork.doctor-service.service-discovery.consul-port=${CONSUL_PORT:8500}
     */
}
