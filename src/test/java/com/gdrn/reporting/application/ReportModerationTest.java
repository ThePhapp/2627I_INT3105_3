package com.gdrn.reporting.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.gdrn.reporting.application.contract.*;
import com.gdrn.reporting.application.port.*;
import com.gdrn.reporting.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReportModerationTest {
    final ReportStore store = mock(ReportStore.class);
    final DisasterLookup disasters = mock(DisasterLookup.class);
    final Instant now = Instant.parse("2026-10-10T00:00:00Z");
    final ReportModerationService service = new ReportModerationService(store, disasters, Clock.fixed(now, ZoneOffset.UTC));
    final ReportActor citizen = new ReportActor(UUID.randomUUID(), ReportActor.Role.CITIZEN);
    final ReportActor authority = new ReportActor(UUID.randomUUID(), ReportActor.Role.AUTHORITY);
    final UUID disasterId = UUID.randomUUID();
    final Report pending = Report.submit(UUID.randomUUID(), citizen.id(), ReportType.FLOOD, "Help", new Coordinates(0, 0), now);
    final ReportModerationService.Decision verify = new ReportModerationService.Decision(ReportStatus.VERIFIED, disasterId, null);
    final ReportModerationService.Decision reject = new ReportModerationService.Decision(ReportStatus.REJECTED, null, "Reason");

    @Test void rolesAreEnforcedBeforeAnyPortAccessForBothCommands() {
        assertThatThrownBy(() -> service.decide(citizen, pending.id(), verify)).isInstanceOf(ReportAccessDenied.class);
        assertThatThrownBy(() -> service.decide(citizen, pending.id(), reject)).isInstanceOf(ReportAccessDenied.class);
        assertThatThrownBy(() -> service.withdraw(authority, pending.id())).isInstanceOf(ReportAccessDenied.class);
        verifyNoInteractions(store, disasters);
    }

    @Test void visibilityAndPendingAreCheckedBeforeDisasterAndWrites() {
        when(store.findById(pending.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.decide(authority, pending.id(), verify)).isInstanceOf(ReportNotFound.class);
        when(store.findById(pending.id())).thenReturn(Optional.of(pending.reject("reason", now)));
        assertThatThrownBy(() -> service.decide(authority, pending.id(), verify)).isInstanceOf(InvalidReportTransition.class);
        var other = new ReportActor(UUID.randomUUID(), ReportActor.Role.CITIZEN);
        assertThatThrownBy(() -> service.withdraw(other, pending.id())).isInstanceOf(ReportNotFound.class);
        assertThatThrownBy(() -> service.withdraw(citizen, pending.id())).isInstanceOf(InvalidReportTransition.class);
        verifyNoInteractions(disasters);
        verify(store, never()).updatePending(any(), anyLong());
    }

    @Test void verificationRequiresFreshExistingActiveDisasterAndAtomicWrite() {
        when(store.findById(pending.id())).thenReturn(Optional.of(pending));
        when(disasters.findByIds(Set.of(disasterId))).thenReturn(Map.of());
        assertThatThrownBy(() -> service.decide(authority, pending.id(), verify)).isInstanceOf(ReportNotFound.class);
        when(disasters.findByIds(Set.of(disasterId))).thenReturn(Map.of(disasterId, new LinkedDisaster(disasterId, LinkedDisasterState.RESOLVED)));
        assertThatThrownBy(() -> service.decide(authority, pending.id(), verify)).isInstanceOf(DisasterNotActive.class);
        when(disasters.findByIds(Set.of(disasterId))).thenReturn(Map.of(disasterId, new LinkedDisaster(disasterId, LinkedDisasterState.ACTIVE)));
        when(store.updatePending(any(), eq(0L))).thenReturn(true);
        assertThat(service.decide(authority, pending.id(), verify).disasterId()).isEqualTo(disasterId);
        when(store.updatePending(any(), eq(0L))).thenReturn(false);
        assertThatThrownBy(() -> service.decide(authority, pending.id(), verify)).isInstanceOf(InvalidReportTransition.class);
        verify(disasters, times(4)).findByIds(Set.of(disasterId));
    }

    @Test void rejectAndWithdrawDoNotQueryDisastersAndLosingWritesConflict() {
        when(store.findById(pending.id())).thenReturn(Optional.of(pending));
        when(store.updatePending(any(), eq(0L))).thenReturn(true);
        assertThat(service.decide(authority, pending.id(), reject).status()).isEqualTo(ReportStatus.REJECTED);
        service.withdraw(citizen, pending.id());
        when(store.updatePending(any(), eq(0L))).thenReturn(false);
        assertThatThrownBy(() -> service.withdraw(citizen, pending.id())).isInstanceOf(InvalidReportTransition.class);
        verifyNoInteractions(disasters);
    }

    @Test void invalidDecisionCombinationsFail() {
        assertThatThrownBy(() -> new ReportModerationService.Decision(ReportStatus.PENDING, null, null)).isInstanceOf(InvalidReport.class);
        assertThatThrownBy(() -> new ReportModerationService.Decision(ReportStatus.VERIFIED, null, null)).isInstanceOf(InvalidReport.class);
        assertThatThrownBy(() -> new ReportModerationService.Decision(ReportStatus.VERIFIED, disasterId, "reason")).isInstanceOf(InvalidReport.class);
        assertThatThrownBy(() -> new ReportModerationService.Decision(ReportStatus.REJECTED, disasterId, "reason")).isInstanceOf(InvalidReport.class);
    }
}
