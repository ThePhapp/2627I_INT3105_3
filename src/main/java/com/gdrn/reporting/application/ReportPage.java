package com.gdrn.reporting.application;

import com.gdrn.reporting.domain.Report;
import java.util.List;

public record ReportPage(List<Report> items, int page, int size, long totalElements) {
    public ReportPage { items = List.copyOf(items); }
    public long totalPages() { return totalElements / size + (totalElements % size == 0 ? 0 : 1); }
}
