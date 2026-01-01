package com.hospital.appointment.consumer;

import com.hospital.appointment.service.CacheEvictionService;
import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.CacheInvalidationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for cross-service cache invalidation events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationListener {

    private final CacheEvictionService cacheEvictionService;

    @RabbitListener(queues = RabbitMQConfig.CACHE_INVALIDATION_QUEUE)
    public void handleCacheInvalidation(CacheInvalidationEvent event) {
        log.info("Received cache invalidation event for cache(s): {}, invalidateAll: {}",
                event.getCacheNames(), event.isInvalidateAll());

        String[] cacheNames = event.getCacheNames().split(",");

        for (String cacheName : cacheNames) {
            String trimmedCacheName = cacheName.trim();

            if (event.isInvalidateAll()) {
                cacheEvictionService.evictAllFromCache(trimmedCacheName);
            } else if (event.getEntityIds() != null && !event.getEntityIds().isEmpty()) {
                for (var entityId : event.getEntityIds()) {
                    cacheEvictionService.evictFromCache(trimmedCacheName, entityId);
                }
            }
        }

        log.info("Cache invalidation completed for: {}", event.getCacheNames());
    }
}
