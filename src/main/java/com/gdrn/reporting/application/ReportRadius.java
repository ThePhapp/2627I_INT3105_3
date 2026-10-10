package com.gdrn.reporting.application;

import com.gdrn.reporting.domain.Coordinates;
import com.gdrn.reporting.domain.InvalidReport;
import java.util.Objects;

public record ReportRadius(Coordinates center, int meters) {
    public ReportRadius {
        Objects.requireNonNull(center);
        if (meters < 1 || meters > 100_000) throw new InvalidReport("radiusMeters");
    }
}
