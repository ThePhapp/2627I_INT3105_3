package com.gdrn.reporting.application;

import com.gdrn.reporting.application.contract.*;
import com.gdrn.reporting.application.port.DisasterLookup;
import com.gdrn.reporting.application.port.ReportStore;
import java.util.*;
import java.util.stream.Collectors;

public final class PublishedReportingQuery implements ReportingQuery {
    private final ReportStore reports;
    private final DisasterLookup disasters;
    public PublishedReportingQuery(ReportStore reports, DisasterLookup disasters) {
        this.reports = reports; this.disasters = disasters;
    }

    @Override public Optional<ReportSnapshot> findById(UUID id) {
        if (id == null) throw new IllegalArgumentException("Null report ID");
        return Optional.ofNullable(findByIds(Set.of(id)).get(id));
    }

    @Override public Map<UUID, ReportSnapshot> findByIds(Set<UUID> ids) {
        if (ids == null || ids.size() > 100 || ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Expected at most 100 non-null report IDs");
        }
        if (ids.isEmpty()) return Map.of();
        var found = reports.findByIds(Set.copyOf(ids));
        var disasterIds = found.stream().map(report -> report.disasterId()).filter(Objects::nonNull).collect(Collectors.toSet());
        var linked = disasterIds.isEmpty() ? Map.<UUID, LinkedDisaster>of() : disasters.findByIds(disasterIds);
        var result = new HashMap<UUID, ReportSnapshot>();
        for (var report : found) {
            LinkedDisaster disaster = report.disasterId() == null ? null : linked.get(report.disasterId());
            if (report.disasterId() != null && disaster == null) throw new IllegalStateException("Missing linked disaster");
            ReportState state = switch (report.status()) {
                case PENDING -> ReportState.PENDING;
                case VERIFIED -> ReportState.VERIFIED;
                case REJECTED -> ReportState.REJECTED;
            };
            result.put(report.id(), new ReportSnapshot(report.id(), report.reporterId(), state, Optional.ofNullable(disaster)));
        }
        return Map.copyOf(result);
    }
}
