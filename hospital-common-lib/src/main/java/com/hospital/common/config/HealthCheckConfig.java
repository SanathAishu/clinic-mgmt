package com.hospital.common.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

/**
 * Health check endpoints for Kubernetes liveness and readiness probes.
 *
 * Liveness: Is the service running? (if false, restart container)
 * Readiness: Can the service accept traffic? (if false, remove from load balancer)
 *
 * Each service can extend these checks with specific database/Redis/RabbitMQ checks.
 */
@ApplicationScoped
public class HealthCheckConfig {

    /**
     * Liveness probe - checks if the application is running.
     * If this fails, Kubernetes will restart the pod.
     */
    @Liveness
    @ApplicationScoped
    public static class LivenessCheck implements HealthCheck {

        @Override
        public HealthCheckResponse call() {
            // Basic liveness check - just confirms the app is running
            return HealthCheckResponse
                    .named("hospital-service-liveness")
                    .up()
                    .withData("status", "alive")
                    .build();
        }
    }

    /**
     * Readiness probe - checks if the application is ready to accept traffic.
     * If this fails, Kubernetes will remove the pod from service endpoints.
     *
     * Child services should override this to check:
     * - Database connectivity
     * - Redis availability
     * - RabbitMQ connection
     */
    @Readiness
    @ApplicationScoped
    public static class ReadinessCheck implements HealthCheck {

        @Override
        public HealthCheckResponse call() {
            // Basic readiness check
            // Child services will add specific checks for DB, Redis, RabbitMQ
            return HealthCheckResponse
                    .named("hospital-service-readiness")
                    .up()
                    .withData("status", "ready")
                    .build();
        }
    }

    /**
     * Example custom health check for database connectivity.
     * Child services can implement similar checks.
     *
     * Usage in child services:
     *
     * @Readiness
     * @ApplicationScoped
     * public class DatabaseHealthCheck implements HealthCheck {
     *     @Inject
     *     DataSource dataSource;
     *
     *     @Override
     *     public HealthCheckResponse call() {
     *         try (Connection conn = dataSource.getConnection()) {
     *             boolean valid = conn.isValid(5);
     *             return HealthCheckResponse
     *                 .named("database-connection")
     *                 .state(valid)
     *                 .withData("database", "PostgreSQL")
     *                 .build();
     *         } catch (Exception e) {
     *             return HealthCheckResponse
     *                 .named("database-connection")
     *                 .down()
     *                 .withData("error", e.getMessage())
     *                 .build();
     *         }
     *     }
     * }
     */
}
