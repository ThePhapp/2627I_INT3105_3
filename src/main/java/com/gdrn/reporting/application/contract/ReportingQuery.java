package com.gdrn.reporting.application.contract;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ReportingQuery {
    Optional<ReportSnapshot> findById(UUID reportId);
    Map<UUID, ReportSnapshot> findByIds(Set<UUID> reportIds);
}
