package com.gdrn.reporting.application.port;

import com.gdrn.reporting.application.ReportFilter;
import com.gdrn.reporting.application.ReportPage;
import com.gdrn.reporting.domain.Report;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.List;

public interface ReportStore {
    Report insert(Report report);
    Optional<Report> findById(UUID id);
    /** Missing and withdrawn reports are omitted. */
    List<Report> findByIds(Set<UUID> ids);
    /** Single atomic write; false if another command changed/withdrew the observed report. */
    boolean updatePending(Report changed, long expectedVersion);
    /** restrictedReporterId null means authority; filter and count use one DB snapshot. */
    ReportPage find(ReportFilter filter, UUID restrictedReporterId);
}
