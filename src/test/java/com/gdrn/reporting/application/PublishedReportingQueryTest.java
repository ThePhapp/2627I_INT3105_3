package com.gdrn.reporting.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.gdrn.reporting.application.contract.*;
import com.gdrn.reporting.application.port.*;
import com.gdrn.reporting.domain.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PublishedReportingQueryTest {
    final ReportStore store = mock(ReportStore.class);
    final DisasterLookup disasters = mock(DisasterLookup.class);
    final ReportingQuery query = new PublishedReportingQuery(store, disasters);
    final Instant now = Instant.now();
    Report pending() { return Report.submit(UUID.randomUUID(), UUID.randomUUID(), ReportType.FLOOD, "Help", new Coordinates(0, 0), now); }

    @Test void validatesBatchLimitsAndReturnsImmutableEmptyWithoutQueries() {
        assertThat(query.findByIds(Set.of())).isEmpty();
        assertThatThrownBy(() -> query.findByIds(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> query.findById(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> query.findByIds(new HashSet<>(Arrays.asList((UUID) null)))).isInstanceOf(IllegalArgumentException.class);
        var tooMany = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toSet());
        assertThatThrownBy(() -> query.findByIds(tooMany)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(store, disasters);
    }

    @Test void batchesDistinctReferencesOnceAndReturnsCurrentResolvedSnapshot() {
        var disaster = UUID.randomUUID();
        var first = pending().verify(disaster, now);
        var second = pending().verify(disaster, now);
        var pending = pending();
        var rejected = pending().reject("Reason", now);
        var missing = UUID.randomUUID();
        var ids = Set.of(first.id(), second.id(), pending.id(), rejected.id(), missing);
        when(store.findByIds(ids)).thenReturn(List.of(first, second, pending, rejected));
        when(disasters.findByIds(Set.of(disaster))).thenReturn(Map.of(disaster, new LinkedDisaster(disaster, LinkedDisasterState.RESOLVED)));
        var result = query.findByIds(ids);
        assertThat(result).hasSize(4).doesNotContainKey(missing);
        assertThat(result.get(first.id()).disaster().orElseThrow().status()).isEqualTo(LinkedDisasterState.RESOLVED);
        assertThat(result.get(pending.id()).disaster()).isEmpty();
        assertThat(result.get(rejected.id()).status()).isEqualTo(ReportState.REJECTED);
        assertThatThrownBy(result::clear).isInstanceOf(UnsupportedOperationException.class);
        verify(store).findByIds(ids);
        verify(disasters).findByIds(Set.of(disaster));
    }

    @Test void absenceIsDistinctFromBrokenReferenceOrPortFailure() {
        var id = UUID.randomUUID();
        when(store.findByIds(Set.of(id))).thenReturn(List.of());
        assertThat(query.findById(id)).isEmpty();
        verifyNoInteractions(disasters);
        var verified = pending().verify(UUID.randomUUID(), now);
        when(store.findByIds(Set.of(verified.id()))).thenReturn(List.of(verified));
        when(disasters.findByIds(Set.of(verified.disasterId()))).thenReturn(Map.of());
        assertThatThrownBy(() -> query.findById(verified.id())).isInstanceOf(IllegalStateException.class);
        when(store.findByIds(Set.of(id))).thenThrow(new IllegalStateException("store failed"));
        assertThatThrownBy(() -> query.findById(id)).isInstanceOf(IllegalStateException.class);
    }
}
