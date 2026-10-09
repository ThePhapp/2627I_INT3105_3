package com.gdrn.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.gdrn.reporting.application.port.ReportStore;
import com.gdrn.reporting.domain.Coordinates;
import com.gdrn.reporting.domain.InvalidReport;
import com.gdrn.reporting.domain.Report;
import com.gdrn.reporting.domain.ReportType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportServiceTest {
    final ReportStore store = mock(ReportStore.class);
    final Instant now = Instant.parse("2026-10-09T02:00:00.123456789Z");
    final ReportService service = new ReportService(store, Clock.fixed(now, ZoneOffset.UTC));
    final ReportActor citizen = new ReportActor(UUID.randomUUID(), ReportActor.Role.CITIZEN);
    final ReportActor authority = new ReportActor(UUID.randomUUID(), ReportActor.Role.AUTHORITY);
    final ReportFilter filter = new ReportFilter(0, 20, ReportFilter.Sort.CREATED_DESC, null, null, null);

    @Test void submissionUsesActorAndClockAndRejectsAuthorityBeforeWriting() {
        when(store.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var report = service.submit(citizen, ReportType.FLOOD, "Help", new Coordinates(0, 0));
        assertThat(report.reporterId()).isEqualTo(citizen.id());
        assertThat(report.createdAt()).isEqualTo(Instant.parse("2026-10-09T02:00:00.123456Z"));
        assertThatThrownBy(() -> service.submit(authority, ReportType.FLOOD, "Help", new Coordinates(0, 0)))
                .isInstanceOf(ReportAccessDenied.class);
        verify(store, times(1)).insert(any());
    }

    @Test void otherOwnersAreIndistinguishableFromMissingAndAuthorityCanRead() {
        var report = Report.submit(UUID.randomUUID(), citizen.id(), ReportType.FLOOD, "Help", new Coordinates(0, 0), now);
        when(store.findById(report.id())).thenReturn(Optional.of(report));
        assertThat(service.detail(citizen, report.id())).isSameAs(report);
        assertThat(service.detail(authority, report.id())).isSameAs(report);
        var other = new ReportActor(UUID.randomUUID(), ReportActor.Role.CITIZEN);
        assertThatThrownBy(() -> service.detail(other, report.id())).isInstanceOf(ReportNotFound.class);
        var missing = UUID.randomUUID();
        when(store.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(citizen, missing)).isInstanceOf(ReportNotFound.class);
    }

    @Test void listRestrictsCitizenAtStoreButNotAuthority() {
        when(store.find(any(), any())).thenReturn(new ReportPage(List.of(), 0, 20, 0));
        service.list(citizen, filter);
        service.list(authority, filter);
        verify(store).find(filter, citizen.id());
        verify(store).find(filter, null);
    }

    @Test void paginationRejectsLimitsAndDoesNotClamp() {
        for (int page : new int[]{-1, 1_000_001}) {
            assertThatThrownBy(() -> new ReportFilter(page, 20, ReportFilter.Sort.CREATED_DESC, null, null, null))
                    .isInstanceOf(InvalidReport.class);
        }
        for (int size : new int[]{0, 101}) {
            assertThatThrownBy(() -> new ReportFilter(0, size, ReportFilter.Sort.CREATED_ASC, null, null, null))
                    .isInstanceOf(InvalidReport.class);
        }
        assertThat(new ReportPage(List.of(), 0, 20, 0).totalPages()).isZero();
        assertThat(new ReportPage(List.of(), 0, 20, 21).totalPages()).isEqualTo(2);
    }
}
