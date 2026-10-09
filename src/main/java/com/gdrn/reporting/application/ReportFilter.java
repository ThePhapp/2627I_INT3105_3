package com.gdrn.reporting.application;

import com.gdrn.reporting.domain.InvalidReport;
import com.gdrn.reporting.domain.ReportStatus;
import com.gdrn.reporting.domain.ReportType;
import java.util.UUID;

/** Null optional filters mean absent; ownership is deliberately not a client filter. */
public record ReportFilter(int page, int size, Sort sort, ReportStatus status, ReportType type, UUID disasterId) {
    public enum Sort { CREATED_DESC, CREATED_ASC }

    public ReportFilter {
        if (page < 0 || page > 1_000_000) throw new InvalidReport("page");
        if (size < 1 || size > 100) throw new InvalidReport("size");
        if (sort == null) throw new InvalidReport("sort");
    }
}
