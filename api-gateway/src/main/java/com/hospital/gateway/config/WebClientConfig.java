package com.hospital.gateway.config;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * WebClient Configuration for API Gateway.
 *
 * Configures Vert.x WebClient for making HTTP requests to backend services.
 *
 * Features:
 * - Connection pooling
 * - Timeouts (connect: 5s, idle: 30s)
 * - Keep-alive connections
 * - Automatic retry (not implemented yet, can be added)
 */
@ApplicationScoped
public class WebClientConfig {

    @Inject
    Vertx vertx;

    /**
     * Produce WebClient bean for dependency injection.
     *
     * @return Configured WebClient instance
     */
    @Produces
    @ApplicationScoped
    public WebClient webClient() {
        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout(5000)        // 5 seconds to connect
                .setIdleTimeout(30)             // 30 seconds idle timeout
                .setKeepAlive(true)             // Keep connections alive
                .setMaxPoolSize(100)            // Max 100 connections per host
                .setHttp2MaxPoolSize(50)        // Max 50 HTTP/2 connections
                .setFollowRedirects(false)      // Don't follow redirects
                .setTryUseCompression(true);    // Use compression if supported

        return WebClient.create(vertx, options);
    }
}
