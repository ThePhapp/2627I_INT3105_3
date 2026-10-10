package com.gdrn.reporting.domain;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportLifecycleTest {
    final Instant now = Instant.parse("2026-10-10T00:00:00Z");
    final Report pending = Report.submit(UUID.randomUUID(), UUID.randomUUID(), ReportType.FLOOD,
            "Flood", new Coordinates(0, 0), now);

    @Test void terminalDecisionsAndWithdrawalCannotBeRepeatedOrChanged() {
        var verified = pending.verify(UUID.randomUUID(), now.plusSeconds(1));
        var rejected = pending.reject("  Insufficient details  ", now.plusSeconds(1));
        var withdrawn = pending.withdraw(now.plusSeconds(1));
        assertThat(verified.status()).isEqualTo(ReportStatus.VERIFIED);
        assertThat(verified.verifiedAt()).isEqualTo(verified.updatedAt());
        assertThat(rejected.rejectionReason()).isEqualTo("Insufficient details");
        assertThat(rejected.rejectedAt()).isEqualTo(rejected.updatedAt());
        assertThat(withdrawn.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(withdrawn.withdrawnAt()).isEqualTo(withdrawn.updatedAt());
        for (var report : new Report[]{verified, rejected, withdrawn}) {
            assertThat(report.version()).isEqualTo(1);
            assertThatThrownBy(() -> report.verify(UUID.randomUUID(), now)).isInstanceOf(InvalidReportTransition.class);
            assertThatThrownBy(() -> report.reject("reason", now)).isInstanceOf(InvalidReportTransition.class);
            assertThatThrownBy(() -> report.withdraw(now)).isInstanceOf(InvalidReportTransition.class);
        }
    }

    @Test void reasonUsesCodePointsAndRejectsBlankMissingAndOverLimit() {
        for (String reason : new String[]{null, "  \n", "a".repeat(501)}) {
            assertThatThrownBy(() -> pending.reject(reason, now)).isInstanceOf(InvalidReport.class);
        }
        assertThat(pending.reject("😀".repeat(500), now).rejectionReason()).isEqualTo("😀".repeat(500));
        assertThatThrownBy(() -> pending.verify(null, now)).isInstanceOf(InvalidReport.class);
        assertThatThrownBy(() -> pending.withdraw(now.minusSeconds(1))).isInstanceOf(InvalidReport.class);
    }
}
