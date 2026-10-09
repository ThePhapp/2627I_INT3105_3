package com.gdrn.disaster.application;

import com.gdrn.disaster.application.contract.*;
import com.gdrn.disaster.application.port.DisasterRepository;
import com.gdrn.disaster.domain.Disaster;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PublishedDisasterQuery implements DisasterQuery {
    private final DisasterRepository repository;
    public PublishedDisasterQuery(DisasterRepository repository) { this.repository = repository; }

    @Override public Optional<DisasterSnapshot> findById(UUID disasterId) {
        if (disasterId == null) throw new IllegalArgumentException("disasterId is required");
        return repository.findById(disasterId).map(PublishedDisasterQuery::snapshot);
    }

    @Override public Map<UUID, DisasterSnapshot> findByIds(Set<UUID> disasterIds) {
        if (disasterIds == null || disasterIds.size() > 100 || disasterIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("1 to 100 non-null IDs are required");
        }
        if (disasterIds.isEmpty()) return Map.of();
        Map<UUID, DisasterSnapshot> result = new LinkedHashMap<>();
        repository.findByIds(Set.copyOf(disasterIds)).forEach((id, disaster) -> result.put(id, snapshot(disaster)));
        return Map.copyOf(result);
    }

    private static DisasterSnapshot snapshot(Disaster disaster) {
        return new DisasterSnapshot(disaster.id(), DisasterState.valueOf(disaster.status().name()));
    }
}
