package com.gdrn.disaster.application.contract;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DisasterQuery {
    Optional<DisasterSnapshot> findById(UUID disasterId);
    Map<UUID, DisasterSnapshot> findByIds(Set<UUID> disasterIds);
}
