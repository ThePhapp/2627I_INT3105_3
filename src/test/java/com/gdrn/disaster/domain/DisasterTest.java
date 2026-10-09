package com.gdrn.disaster.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class DisasterTest {
    private final Instant created = Instant.parse("2026-10-09T01:00:00Z");

    @Test void createsActiveNormalizedDisasterAndValidatesFields() {
        var disaster = disaster();
        assertThat(disaster.name()).isEqualTo("Hanoi flood");
        assertThat(disaster.status()).isEqualTo(DisasterStatus.ACTIVE);
        assertThat(disaster.version()).isZero();
        assertThatThrownBy(() -> Disaster.create(UUID.randomUUID(), " ", DisasterType.FLOOD,
                Severity.HIGH, "description", 0, 0, created)).isInstanceOf(InvalidDisaster.class);
        assertThatThrownBy(() -> Disaster.create(UUID.randomUUID(), "name", DisasterType.FLOOD,
                Severity.HIGH, "description", Double.NaN, 0, created)).isInstanceOf(InvalidDisaster.class);
        assertThatThrownBy(() -> Disaster.create(UUID.randomUUID(), "name", DisasterType.FLOOD,
                Severity.HIGH, "description", 0, 181, created)).isInstanceOf(InvalidDisaster.class);
    }

    @Test void editsActiveAndIncrementsVersionEvenWhenValueIsUnchanged() {
        var changed = disaster().update(0, change(Optional.of("Hanoi flood"), Optional.empty()), created.plusSeconds(1));
        assertThat(changed.version()).isEqualTo(1);
        assertThat(changed.updatedAt()).isEqualTo(created.plusSeconds(1));
    }

    @Test void resolvesOnceAndResolvedIsImmutable() {
        var resolved = disaster().update(0, change(Optional.empty(), Optional.of(DisasterStatus.RESOLVED)), created.plusSeconds(1));
        assertThat(resolved.status()).isEqualTo(DisasterStatus.RESOLVED);
        assertThatThrownBy(() -> resolved.update(1, change(Optional.of("other"), Optional.empty()), created.plusSeconds(2)))
                .isInstanceOf(InvalidDisasterTransition.class);
        assertThatThrownBy(() -> disaster().update(0, change(Optional.empty(), Optional.of(DisasterStatus.ACTIVE)), created.plusSeconds(1)))
                .isInstanceOf(InvalidDisasterTransition.class);
    }

    @Test void rejectsStaleVersionAndEmptyPatch() {
        assertThatThrownBy(() -> disaster().update(1, change(Optional.of("other"), Optional.empty()), created))
                .isInstanceOf(StaleDisasterVersion.class);
        assertThatThrownBy(() -> disaster().update(0, change(Optional.empty(), Optional.empty()), created))
                .isInstanceOf(InvalidDisaster.class);
    }

    private Disaster disaster() {
        return Disaster.create(UUID.randomUUID(), "  Hanoi flood  ", DisasterType.FLOOD, Severity.HIGH,
                " Training scenario ", 21.028, 105.834, created);
    }
    private Disaster.Change change(Optional<String> name, Optional<DisasterStatus> status) {
        return new Disaster.Change(name, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), status);
    }
}
