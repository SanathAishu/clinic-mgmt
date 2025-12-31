package com.hospital.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

/**
 * Event published to invalidate cache entries across services
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CacheInvalidationEvent extends BaseEvent {
    private String cacheNames;
    private List<UUID> entityIds;
    private boolean invalidateAll;
}
