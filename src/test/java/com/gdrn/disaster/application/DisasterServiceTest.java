package com.gdrn.disaster.application;

import com.gdrn.disaster.application.contract.DisasterState;
import com.gdrn.disaster.application.port.DisasterRepository;
import com.gdrn.disaster.domain.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class DisasterServiceTest {
    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000004");
    private final FakeRepository repository = new FakeRepository();
    private final DisasterService service = new DisasterService(repository, () -> ID,
            Clock.fixed(Instant.parse("2026-10-09T01:00:00Z"), ZoneOffset.UTC));

    @Test void createsGetsListsAndUpdatesThroughPort() {
        var created = service.create(new DisasterService.Create("Flood", DisasterType.FLOOD,
                Severity.HIGH, "Training", 21, 105));
        assertThat(created.id()).isEqualTo(ID);
        assertThat(service.get(ID)).isSameAs(created);
        assertThat(service.list(new DisasterSearch(0, 20, DisasterSearch.Sort.CREATED_AT_DESC,
                Optional.empty(), Optional.empty())).items()).containsExactly(created);
        var updated = service.update(new DisasterService.Update(ID, 0, new Disaster.Change(
                Optional.empty(), Optional.empty(), Optional.of(Severity.CRITICAL), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty())));
        assertThat(updated.severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test void reportsMissingAndConcurrentVersionLoss() {
        assertThatThrownBy(() -> service.get(ID)).isInstanceOf(DisasterNotFound.class);
        var created = service.create(new DisasterService.Create("Flood", DisasterType.FLOOD,
                Severity.HIGH, "Training", 21, 105));
        repository.acceptUpdate = false;
        assertThatThrownBy(() -> service.update(new DisasterService.Update(created.id(), 0,
                new Disaster.Change(Optional.of("Changed"), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()))))
                .isInstanceOf(StaleDisasterVersion.class);
    }

    @Test void publishedContractValidatesBatchAndMapsOnlyExistingSnapshots() {
        var created = service.create(new DisasterService.Create("Flood", DisasterType.FLOOD,
                Severity.HIGH, "Training", 21, 105));
        var query = new PublishedDisasterQuery(repository);
        assertThat(query.findById(ID).orElseThrow().status()).isEqualTo(DisasterState.ACTIVE);
        UUID missing = UUID.randomUUID();
        assertThat(query.findByIds(Set.of(ID, missing))).containsOnlyKeys(ID);
        assertThat(query.findByIds(Set.of())).isEmpty();
        assertThatThrownBy(() -> query.findByIds(null)).isInstanceOf(IllegalArgumentException.class);
        Set<UUID> tooMany = new HashSet<>();
        for (int index = 0; index < 101; index++) tooMany.add(UUID.randomUUID());
        assertThatThrownBy(() -> query.findByIds(tooMany)).isInstanceOf(IllegalArgumentException.class);
        service.update(new DisasterService.Update(created.id(), 0, new Disaster.Change(Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(DisasterStatus.RESOLVED))));
        assertThat(query.findById(ID).orElseThrow().status()).isEqualTo(DisasterState.RESOLVED);
    }

    private static final class FakeRepository implements DisasterRepository {
        private final Map<UUID, Disaster> values = new HashMap<>();
        private boolean acceptUpdate = true;
        public void add(Disaster disaster) { values.put(disaster.id(), disaster); }
        public Optional<Disaster> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        public Map<UUID, Disaster> findByIds(Set<UUID> ids) {
            Map<UUID, Disaster> result = new HashMap<>();
            ids.forEach(id -> { if (values.containsKey(id)) result.put(id, values.get(id)); });
            return result;
        }
        public DisasterPage search(DisasterSearch search) {
            return new DisasterPage(new ArrayList<>(values.values()), search.page(), search.size(), values.size(), values.isEmpty() ? 0 : 1);
        }
        public boolean update(Disaster disaster, long expectedVersion) {
            if (!acceptUpdate) return false;
            values.put(disaster.id(), disaster); return true;
        }
    }
}
