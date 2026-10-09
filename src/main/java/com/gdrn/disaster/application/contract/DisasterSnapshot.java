package com.gdrn.disaster.application.contract;

import java.util.Objects;
import java.util.UUID;

public record DisasterSnapshot(UUID id, DisasterState status) {
    public DisasterSnapshot {
        Objects.requireNonNull(id);
        Objects.requireNonNull(status);
    }
}
