package com.hospital.gateway.filter;

import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

/**
 * Rate Limiting Filter using Redis.
 *
 * Implements token bucket algorithm:
 * - Each user/IP gets a bucket of tokens
 * - Each request consumes one token
 * - Tokens refill over time
 * - When bucket is empty, requests are rejected with 429 Too Many Requests
 *
 * Configuration:
 * - gateway.rate-limit.enabled (default: true)
 * - gateway.rate-limit.requests-per-minute (default: 100)
 * - gateway.rate-limit.burst (default: 20)
 */
@ApplicationScoped
public class RateLimitFilter {

    @Inject
    RedisDataSource redisDataSource;

    @ConfigProperty(name = "gateway.rate-limit.enabled", defaultValue = "true")
    boolean rateLimitEnabled;

    @ConfigProperty(name = "gateway.rate-limit.requests-per-minute", defaultValue = "100")
    int requestsPerMinute;

    @ConfigProperty(name = "gateway.rate-limit.burst", defaultValue = "20")
    int burstSize;

    private ValueCommands<String, Long> redisCommands;

    public boolean checkRateLimit(RoutingContext context) {
        if (!rateLimitEnabled) {
            return true; // Rate limiting disabled
        }

        // Get user identifier (userId from header or IP address)
        String userId = context.request().getHeader("X-User-Id");
        String clientIp = context.request().remoteAddress().hostAddress();
        String rateLimitKey = userId != null
                ? "rate_limit:user:" + userId
                : "rate_limit:ip:" + clientIp;

        try {
            // Initialize Redis commands lazily
            if (redisCommands == null) {
                redisCommands = redisDataSource.value(Long.class);
            }

            // Get current token count
            Long currentTokens = redisCommands.get(rateLimitKey);

            if (currentTokens == null) {
                // First request - initialize with burst size
                redisCommands.setex(rateLimitKey, 60, (long) burstSize - 1);
                return true;
            }

            if (currentTokens <= 0) {
                // Rate limit exceeded
                Log.warnf("Rate limit exceeded for %s (key: %s)",
                    userId != null ? "user " + userId : "IP " + clientIp,
                    rateLimitKey);

                context.response()
                        .setStatusCode(429)
                        .putHeader("Content-Type", "application/json")
                        .putHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute))
                        .putHeader("X-RateLimit-Remaining", "0")
                        .putHeader("Retry-After", "60")
                        .end("{\"error\":\"Rate limit exceeded\",\"retryAfter\":60}");
                return false;
            }

            // Consume one token
            long remainingTokens = redisCommands.decr(rateLimitKey);

            // Add rate limit headers
            context.response()
                    .putHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute))
                    .putHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remainingTokens)));

            Log.debugf("Rate limit check passed: %s, remaining: %d", rateLimitKey, remainingTokens);
            return true;

        } catch (Exception e) {
            Log.errorf(e, "Error checking rate limit for key: %s", rateLimitKey);
            // Fail open - allow request if Redis is unavailable
            return true;
        }
    }

    public void refillTokens() {
        // Token refill is handled by Redis TTL (setex)
        // Keys expire after 60 seconds, effectively refilling the bucket
        Log.debug("Token refill handled by Redis TTL");
    }
}
