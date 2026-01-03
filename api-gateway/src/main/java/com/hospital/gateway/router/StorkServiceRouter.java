package com.hospital.gateway.router;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceInstance;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Service Router using SmallRye Stork for dynamic service discovery.
 *
 * Instead of hardcoded URLs, Stork dynamically discovers service instances
 * from Consul and provides load balancing.
 *
 * Benefits over hardcoded URLs:
 * 1. Dynamic service discovery - services can be added/removed without config changes
 * 2. Load balancing - distributes requests across multiple instances
 * 3. Health checking - only routes to healthy instances
 * 4. Fault tolerance - automatic failover if instance goes down
 * 5. Service mesh ready - works with Consul, Kubernetes, etc.
 *
 * Architecture:
 * Request → Gateway → Stork → Consul → Service Instance(s)
 *
 * Example:
 * GET /api/patients/123
 *   → Stork resolves "patient-service" from Consul
 *   → Gets healthy instance: http://10.0.0.5:8082
 *   → Routes to: http://10.0.0.5:8082/api/patients/123
 */
@ApplicationScoped
public class StorkServiceRouter {

    @Inject
    WebClient webClient;

    // Stork instance - accessed programmatically to avoid CDI issues
    private final Stork stork = Stork.getInstance();

    /**
     * Route request to backend service using Stork service discovery.
     *
     * @param context Vert.x routing context
     */
    public void route(RoutingContext context) {
        String path = context.request().path();
        String serviceName = getServiceName(path);

        if (serviceName == null) {
            context.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"Service not found for path: " + path + "\"}");
            return;
        }

        HttpMethod method = context.request().method();
        Log.debugf("Routing %s %s via Stork service: %s", method, path, serviceName);

        // Get request body
        context.request().body().onSuccess(buffer -> {
            // Use Stork to get service instance
            stork.getService(serviceName)
                    .selectInstanceAndRecordStart(false)
                    .subscribe().with(
                    serviceInstance -> {
                        // Build target URL from discovered service instance
                        String targetUrl = String.format("http://%s:%d%s",
                                serviceInstance.getHost(),
                                serviceInstance.getPort(),
                                path);

                        Log.debugf("Stork resolved %s to %s", serviceName, targetUrl);

                        // Create request to backend service
                        var request = webClient.requestAbs(method, targetUrl);

                        // Forward all headers
                        context.request().headers().forEach(header ->
                                request.putHeader(header.getKey(), header.getValue())
                        );

                        // Send request
                        var sendFuture = buffer.length() > 0
                                ? request.sendBuffer(buffer)
                                : request.send();

                        sendFuture.onSuccess(response -> {
                            // Forward response
                            context.response().setStatusCode(response.statusCode());
                            response.headers().forEach(header ->
                                    context.response().putHeader(header.getKey(), header.getValue())
                            );
                            context.response().end(response.bodyAsBuffer());

                            Log.debugf("Routed %s %s → %d via %s",
                                    method, path, response.statusCode(), targetUrl);

                        }).onFailure(error -> {
                            Log.errorf(error, "Error routing %s %s to %s", method, path, targetUrl);
                            context.response()
                                    .setStatusCode(503)
                                    .putHeader("Content-Type", "application/json")
                                    .end("{\"error\":\"Service unavailable: " + error.getMessage() + "\"}");
                        });
                    },
                    error -> {
                        Log.errorf(error, "Stork failed to resolve service: %s", serviceName);
                        context.response()
                                .setStatusCode(503)
                                .putHeader("Content-Type", "application/json")
                                .end("{\"error\":\"Service discovery failed: " + error.getMessage() + "\"}");
                    }
            );

        }).onFailure(error -> {
            Log.errorf(error, "Error reading request body for %s %s", method, path);
            context.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"Invalid request body\"}");
        });
    }

    /**
     * Get Stork service name based on request path.
     *
     * @param path Request path
     * @return Stork service name or null if not found
     */
    private String getServiceName(String path) {
        if (path.startsWith("/api/auth")) {
            return "auth-service";
        } else if (path.startsWith("/api/patients")) {
            return "patient-service";
        } else if (path.startsWith("/api/doctors")) {
            return "doctor-service";
        } else if (path.startsWith("/api/appointments")) {
            return "appointment-service";
        } else if (path.startsWith("/api/medical-records")) {
            return "medical-records-service";
        } else if (path.startsWith("/api/facilities") || path.startsWith("/api/rooms")) {
            return "facility-service";
        } else if (path.startsWith("/api/notifications")) {
            return "notification-service";
        } else if (path.startsWith("/api/audit")) {
            return "audit-service";
        }
        return null;
    }

    /**
     * Get service routing map for monitoring/debugging.
     *
     * @return Map of path prefix to Stork service name
     */
    public Map<String, String> getServiceMap() {
        return Map.of(
                "/api/auth", "auth-service (via Stork)",
                "/api/patients", "patient-service (via Stork)",
                "/api/doctors", "doctor-service (via Stork)",
                "/api/appointments", "appointment-service (via Stork)",
                "/api/medical-records", "medical-records-service (via Stork)",
                "/api/facilities", "facility-service (via Stork)",
                "/api/notifications", "notification-service (via Stork)",
                "/api/audit", "audit-service (via Stork)"
        );
    }
}
