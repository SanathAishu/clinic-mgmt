package com.hospital.gateway.health;

import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Health Check for Backend Services.
 *
 * Checks if backend microservices are reachable.
 * This is a readiness check - gateway is not ready if backend services are down.
 *
 * Checked services:
 * - Auth Service (8081)
 * - Patient Service (8082)
 * - Doctor Service (8083)
 * - Appointment Service (8084)
 *
 * Note: We only check critical services to avoid slow health checks.
 */
@Readiness
@ApplicationScoped
public class BackendServicesHealthCheck implements HealthCheck {

    @Inject
    WebClient webClient;

    @ConfigProperty(name = "services.auth-service.url")
    String authServiceUrl;

    @ConfigProperty(name = "services.patient-service.url")
    String patientServiceUrl;

    @ConfigProperty(name = "services.doctor-service.url")
    String doctorServiceUrl;

    @ConfigProperty(name = "services.appointment-service.url")
    String appointmentServiceUrl;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("backend-services");

        try {
            // Check critical services (with 2 second timeout)
            boolean authUp = checkService(authServiceUrl + "/q/health/live");
            boolean patientUp = checkService(patientServiceUrl + "/q/health/live");
            boolean doctorUp = checkService(doctorServiceUrl + "/q/health/live");
            boolean appointmentUp = checkService(appointmentServiceUrl + "/q/health/live");

            // Add status for each service
            builder.withData("auth-service", authUp ? "UP" : "DOWN");
            builder.withData("patient-service", patientUp ? "UP" : "DOWN");
            builder.withData("doctor-service", doctorUp ? "UP" : "DOWN");
            builder.withData("appointment-service", appointmentUp ? "UP" : "DOWN");

            // Gateway is UP if at least auth service is UP
            // (Other services can be degraded without affecting login)
            boolean isUp = authUp;

            return isUp ? builder.up().build() : builder.down().build();

        } catch (Exception e) {
            return builder.down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }

    private boolean checkService(String healthUrl) {
        try {
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            webClient.getAbs(healthUrl)
                    .timeout(2000) // 2 second timeout
                    .send()
                    .onSuccess(response -> future.complete(response.statusCode() == 200))
                    .onFailure(error -> future.complete(false));

            return future.get(3, TimeUnit.SECONDS);

        } catch (Exception e) {
            return false;
        }
    }
}
