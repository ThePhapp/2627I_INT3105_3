package com.gdrn.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ReportTest {
    private Report submit(String description) {
        return Report.submit(UUID.randomUUID(), UUID.randomUUID(), ReportType.FLOOD, description,
                new Coordinates(21.028, 105.834), Instant.parse("2026-10-09T01:00:00Z"));
    }

    @Test void startsPendingWithServerTimesAndTrimmedDescription() {
        var report = submit("  Water rising  \n");
        assertThat(report.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.description()).isEqualTo("Water rising");
        assertThat(report.updatedAt()).isEqualTo(report.createdAt());
        assertThat(report.id()).isNotNull();
        assertThat(report.reporterId()).isNotNull();
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\n\t", "\u2003"})
    void refusesBlankDescriptions(String text) {
        assertThatThrownBy(() -> submit(text)).isInstanceOf(InvalidReport.class);
    }

    @Test void limitsDescriptionByUnicodeCodePointsAfterTrimming() {
        String text = "\uD83C\uDF0A".repeat(2000);
        assertThat(submit(" " + text + " ").description()).isEqualTo(text);
        assertThatThrownBy(() -> submit(text + "x")).isInstanceOf(InvalidReport.class);
    }

    @Test void acceptsBoundaryCoordinatesAndRejectsNonFiniteOrOutOfRange() {
        assertThat(new Coordinates(-90, -180)).isNotNull();
        assertThat(new Coordinates(90, 180)).isNotNull();
        for (double latitude : new double[]{-90.001, 90.001, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> new Coordinates(latitude, 0)).isInstanceOf(InvalidReport.class);
        }
        for (double longitude : new double[]{-180.001, 180.001, Double.NaN, Double.NEGATIVE_INFINITY}) {
            assertThatThrownBy(() -> new Coordinates(0, longitude)).isInstanceOf(InvalidReport.class);
        }
    }

    @Test void reconstitutionPreservesIdentityAndTimesAndValidatesInvariants() {
        var report = submit("Example");
        var restored = Report.reconstitutePending(report.id(), report.reporterId(), report.type(),
                report.description(), report.coordinates(), report.createdAt(), report.updatedAt());
        assertThat(restored).usingRecursiveComparison().isEqualTo(report);
        assertThatThrownBy(() -> Report.reconstitutePending(report.id(), report.reporterId(), report.type(),
                report.description(), report.coordinates(), report.createdAt(), report.createdAt().minusSeconds(1)))
                .isInstanceOf(InvalidReport.class);
        assertThatThrownBy(() -> Report.submit(report.id(), report.reporterId(), null, "example",
                report.coordinates(), report.createdAt())).isInstanceOf(InvalidReport.class);
    }
}
