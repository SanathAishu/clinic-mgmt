package com.hospital.gateway.router;

import io.quarkus.logging.Log;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

/**
 * Service Router for API Gateway.
 *
 * Routes incoming requests to backend microservices based on path prefix:
 * - /api/auth/** -> auth-service (port 8081)
 * - /api/patients/** -> patient-service (port 8082)
 * - /api/doctors/** -> doctor-service (port 8083)
 * - /api/appointments/** -> appointment-service (port 8084)
 * - /api/medical-records/** -> medical-records-service (port 8085)
 * - /api/facilities/** -> facility-service (port 8086)
 * - /api/notifications/** -> notification-service (port 8087)
 * - /api/audit/** -> audit-service (port 8088)
 *
 * Features:
 * - Forwards all headers (including X-Tenant-Id, X-User-Id)
 * - Preserves request body
 * - Handles timeouts and errors
 * - Logs request/response for debugging
 */
@ApplicationScoped
public class ServiceRouter {

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

    @ConfigProperty(name = "services.medical-records-service.url")
    String medicalRecordsServiceUrl;

    @ConfigProperty(name = "services.facility-service.url")
    String facilityServiceUrl;

    @ConfigProperty(name = "services.notification-service.url")
    String notificationServiceUrl;

    @ConfigProperty(name = "services.audit-service.url")
    String auditServiceUrl;

    public void route(RoutingContext context) {
        String path = context.request().path();
        String targetServiceUrl = getTargetServiceUrl(path);

        if (targetServiceUrl == null) {
            context.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"Service not found for path: " + path + "\"}");
            return;
        }

        // Build target URL
        String targetUrl = targetServiceUrl + path;
        HttpMethod method = context.request().method();

        Log.debugf("Routing %s %s -> %s", method, path, targetUrl);

        // Get request body (if present)
        context.request().body().onSuccess(buffer -> {
            // Create request to backend service
            var request = webClient.requestAbs(method, targetUrl);

            // Forward all headers (including X-Tenant-Id, X-User-Id, Authorization)
            context.request().headers().forEach(header ->
                    request.putHeader(header.getKey(), header.getValue())
            );

            // Send request with body
            var sendFuture = buffer.length() > 0
                    ? request.sendBuffer(buffer)
                    : request.send();

            sendFuture.onSuccess(response -> {
                // Forward response status and headers
                context.response().setStatusCode(response.statusCode());
                response.headers().forEach(header ->
                        context.response().putHeader(header.getKey(), header.getValue())
                );

                // Forward response body
                context.response().end(response.bodyAsBuffer());

                Log.debugf("Routed %s %s -> %d", method, path, response.statusCode());

            }).onFailure(error -> {
                Log.errorf(error, "Error routing %s %s to %s", method, path, targetUrl);

                context.response()
                        .setStatusCode(503)
                        .putHeader("Content-Type", "application/json")
                        .end("{\"error\":\"Service unavailable: " + error.getMessage() + "\"}");
            });

        }).onFailure(error -> {
            Log.errorf(error, "Error reading request body for %s %s", method, path);
            context.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"Invalid request body\"}");
        });
    }

    private String getTargetServiceUrl(String path) {
        if (path.startsWith("/api/auth")) {
            return authServiceUrl;
        } else if (path.startsWith("/api/patients")) {
            return patientServiceUrl;
        } else if (path.startsWith("/api/doctors")) {
            return doctorServiceUrl;
        } else if (path.startsWith("/api/appointments")) {
            return appointmentServiceUrl;
        } else if (path.startsWith("/api/medical-records")) {
            return medicalRecordsServiceUrl;
        } else if (path.startsWith("/api/facilities") || path.startsWith("/api/rooms")) {
            return facilityServiceUrl;
        } else if (path.startsWith("/api/notifications")) {
            return notificationServiceUrl;
        } else if (path.startsWith("/api/audit")) {
            return auditServiceUrl;
        }
        return null;
    }

    public Map<String, String> getRoutingMap() {
        return Map.of(
                "/api/auth", authServiceUrl,
                "/api/patients", patientServiceUrl,
                "/api/doctors", doctorServiceUrl,
                "/api/appointments", appointmentServiceUrl,
                "/api/medical-records", medicalRecordsServiceUrl,
                "/api/facilities", facilityServiceUrl,
                "/api/notifications", notificationServiceUrl,
                "/api/audit", auditServiceUrl
        );
    }
}
