package com.gdrn.disaster.application.port;

import com.gdrn.disaster.application.DisasterPage;
import com.gdrn.disaster.application.DisasterSearch;
import com.gdrn.disaster.domain.Disaster;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DisasterRepository {
    void add(Disaster disaster);
    Optional<Disaster> findById(UUID id);
    Map<UUID, Disaster> findByIds(Set<UUID> ids);
    DisasterPage search(DisasterSearch search);
    boolean update(Disaster disaster, long expectedVersion);
}
