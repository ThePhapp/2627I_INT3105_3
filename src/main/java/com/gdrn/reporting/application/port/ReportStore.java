package com.gdrn.reporting.application.port;

import com.gdrn.reporting.application.ReportFilter;
import com.gdrn.reporting.application.ReportPage;
import com.gdrn.reporting.domain.Report;
import java.util.Optional;
import java.util.UUID;

public interface ReportStore {
    Report insert(Report report);
    Optional<Report> findById(UUID id);
    /** restrictedReporterId null means authority; filter and count use one DB snapshot. */
    ReportPage find(ReportFilter filter, UUID restrictedReporterId);
}
