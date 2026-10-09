package com.gdrn.reporting.api;

import com.gdrn.reporting.domain.Report;
import com.gdrn.reporting.domain.ReportStatus;
import com.gdrn.reporting.domain.ReportType;
import java.time.Instant;
import java.util.UUID;

public record ReportResponse(UUID id, UUID reporterId, ReportType type, String description,
                             double latitude, double longitude, ReportStatus status,
                             Instant createdAt, Instant updatedAt) {
    static ReportResponse from(Report report) {
        return new ReportResponse(report.id(), report.reporterId(), report.type(), report.description(),
                report.coordinates().latitude(), report.coordinates().longitude(), report.status(),
                report.createdAt(), report.updatedAt());
    }
}
