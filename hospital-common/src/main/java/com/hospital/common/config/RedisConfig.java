package com.hospital.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for distributed caching
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Create ObjectMapper for Redis serialization only (not a Spring bean to avoid
     * interfering with HTTP JSON serialization)
     */
    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(createRedisObjectMapper());

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(createRedisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // Patient Service caches
                .withCacheConfiguration("patients",
                        defaultConfig.entryTtl(Duration.ofHours(1)))
                // Doctor Service caches
                .withCacheConfiguration("doctors",
                        defaultConfig.entryTtl(Duration.ofHours(1)))
                // Appointment Service caches
                .withCacheConfiguration("appointments",
                        defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration("patient-snapshots",
                        defaultConfig.entryTtl(Duration.ofHours(2)))
                .withCacheConfiguration("doctor-snapshots",
                        defaultConfig.entryTtl(Duration.ofHours(2)))
                // Medical Records Service caches
                .withCacheConfiguration("medical-records",
                        defaultConfig.entryTtl(Duration.ofHours(2)))
                .withCacheConfiguration("prescriptions",
                        defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("medicalReports",
                        defaultConfig.entryTtl(Duration.ofHours(2)))
                // Facility Service caches
                .withCacheConfiguration("rooms",
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("room-bookings",
                        defaultConfig.entryTtl(Duration.ofMinutes(15)))
                // Auth Service caches
                .withCacheConfiguration("users",
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("public-keys",
                        defaultConfig.entryTtl(Duration.ofHours(24)))
                .build();
    }
}
