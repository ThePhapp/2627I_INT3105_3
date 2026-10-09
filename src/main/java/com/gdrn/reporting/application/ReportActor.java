package com.gdrn.reporting.application;

import java.util.Objects;
import java.util.UUID;

/** Plain application identity, mapped only from an authenticated principal by the API adapter. */
public record ReportActor(UUID id, Role role) {
    public enum Role { CITIZEN, AUTHORITY }

    public ReportActor {
        Objects.requireNonNull(id);
        Objects.requireNonNull(role);
    }

    public void requireCitizen() {
        if (role != Role.CITIZEN) throw new ReportAccessDenied();
    }

    public boolean canRead(UUID reporterId) {
        return role == Role.AUTHORITY || id.equals(reporterId);
    }
}
