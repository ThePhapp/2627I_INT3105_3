package com.gdrn.reporting.application.contract;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ReportSnapshot(UUID id, UUID reporterId, ReportState status, Optional<LinkedDisaster> disaster) {
    public ReportSnapshot {
        Objects.requireNonNull(id); Objects.requireNonNull(reporterId);
        Objects.requireNonNull(status); Objects.requireNonNull(disaster);
        if ((status == ReportState.VERIFIED) != disaster.isPresent()) throw new IllegalArgumentException("Invalid report snapshot");
    }
}
