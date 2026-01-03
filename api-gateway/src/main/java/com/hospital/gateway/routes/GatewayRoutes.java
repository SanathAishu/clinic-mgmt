package com.hospital.gateway.routes;

import com.hospital.gateway.filter.JwtAuthFilter;
import com.hospital.gateway.filter.RateLimitFilter;
import com.hospital.gateway.router.ServiceRouter;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Main API Gateway Routes Configuration using Vert.x.
 *
 * Request Flow:
 * 1. CORS handling (if cross-origin request)
 * 2. Body parsing (for POST/PUT/PATCH)
 * 3. Timeout handling (30s max)
 * 4. Rate limiting (Redis-based token bucket)
 * 5. JWT authentication (except public paths)
 * 6. Service routing (forward to backend microservices)
 *
 * Architecture:
 * - Loose coupling: Filters are injected, not extended
 * - Composition: Chain handlers via Vert.x routing
 * - Single responsibility: Each filter does one thing
 */
@ApplicationScoped
public class GatewayRoutes {

    @Inject
    Router router;

    @Inject
    JwtAuthFilter jwtAuthFilter;

    @Inject
    RateLimitFilter rateLimitFilter;

    @Inject
    ServiceRouter serviceRouter;

    public void configureRoutes(@Observes StartupEvent event) {
        Log.info("Configuring API Gateway routes...");

        // ===================================================================
        // Global Handlers (applied to all routes)
        // ===================================================================

        // 1. CORS Handler (must be first)
        router.route().handler(CorsHandler.create());

        // 2. Body Handler (parse request body)
        router.route().handler(BodyHandler.create());

        // 3. Timeout Handler (30 seconds max)
        router.route().handler(TimeoutHandler.create(30000));

        // ===================================================================
        // Health Check Routes (no authentication required)
        // ===================================================================

        router.get("/q/health").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end("{\"status\":\"UP\",\"service\":\"api-gateway\"}");
        });

        router.get("/q/health/live").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end("{\"status\":\"UP\"}");
        });

        router.get("/q/health/ready").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end("{\"status\":\"UP\"}");
        });

        // ===================================================================
        // Gateway Info Route
        // ===================================================================

        router.get("/").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end("{\"service\":\"Hospital Management API Gateway\"," +
                         "\"version\":\"1.0.0\"," +
                         "\"status\":\"running\"}");
        });

        router.get("/api/gateway/routes").handler(ctx -> {
            var routes = serviceRouter.getRoutingMap();
            var json = new StringBuilder("{\"routes\":{");
            routes.forEach((path, url) -> {
                json.append("\"").append(path).append("\":\"").append(url).append("\",");
            });
            json.deleteCharAt(json.length() - 1); // Remove last comma
            json.append("}}");

            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(json.toString());
        });

        // ===================================================================
        // API Routes (with authentication and rate limiting)
        // ===================================================================

        router.route("/api/*").handler(ctx -> {
            String path = ctx.request().path();

            Log.debugf("Processing request: %s %s", ctx.request().method(), path);

            // Step 1: Check if public path (login, register, etc.)
            if (jwtAuthFilter.isPublicPath(path)) {
                Log.debugf("Public path, skipping auth: %s", path);
                ctx.next(); // Skip to service routing
                return;
            }

            // Step 2: Rate limiting
            if (!rateLimitFilter.checkRateLimit(ctx)) {
                return; // Rate limit exceeded, response already sent
            }

            // Step 3: JWT authentication
            if (!jwtAuthFilter.authenticate(ctx)) {
                return; // Authentication failed, response already sent
            }

            // Step 4: Forward to service router
            ctx.next();
        });

        // ===================================================================
        // Service Routing (final handler)
        // ===================================================================

        router.route("/api/*").handler(ctx -> {
            serviceRouter.route(ctx);
        });

        // ===================================================================
        // Fallback for unmatched routes
        // ===================================================================

        router.route().handler(ctx -> {
            ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"Route not found\"}");
        });

        Log.info("API Gateway routes configured successfully");
    }
}
