package com.gdrn.reporting.application.contract;

import java.util.Objects;
import java.util.UUID;

public record LinkedDisaster(UUID id, LinkedDisasterState status) {
    public LinkedDisaster { Objects.requireNonNull(id); Objects.requireNonNull(status); }
}
