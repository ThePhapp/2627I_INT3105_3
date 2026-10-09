package com.gdrn.reporting.application;

import com.gdrn.reporting.application.port.ReportStore;
import com.gdrn.reporting.domain.Coordinates;
import com.gdrn.reporting.domain.Report;
import com.gdrn.reporting.domain.ReportType;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class ReportService {
    private final ReportStore reports;
    private final Clock clock;

    public ReportService(ReportStore reports, Clock clock) {
        this.reports = reports;
        this.clock = clock;
    }

    public Report submit(ReportActor actor, ReportType type, String description, Coordinates coordinates) {
        actor.requireCitizen();
        // PostgreSQL stores microseconds; return the same timestamp precision as persisted reads.
        Report report = Report.submit(UUID.randomUUID(), actor.id(), type, description, coordinates,
                clock.instant().truncatedTo(ChronoUnit.MICROS));
        return reports.insert(report);
    }

    public Report detail(ReportActor actor, UUID id) {
        Report report = reports.findById(id).orElseThrow(ReportNotFound::new);
        if (!actor.canRead(report.reporterId())) throw new ReportNotFound();
        return report;
    }

    public ReportPage list(ReportActor actor, ReportFilter filter) {
        return reports.find(filter, actor.role() == ReportActor.Role.CITIZEN ? actor.id() : null);
    }
}
