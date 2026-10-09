package com.gdrn.disaster.application;

import com.gdrn.disaster.domain.DisasterStatus;
import com.gdrn.disaster.domain.DisasterType;
import com.gdrn.disaster.domain.Severity;
import java.util.Locale;

public final class DisasterInput {
    private DisasterInput() {}

    public static DisasterType type(String raw) { return value(raw, DisasterType.class, "type"); }
    public static Severity severity(String raw) { return value(raw, Severity.class, "severity"); }
    public static DisasterStatus status(String raw) { return value(raw, DisasterStatus.class, "status"); }

    private static <T extends Enum<T>> T value(String raw, Class<T> type, String field) {
        if (raw == null || !raw.equals(raw.trim()) || raw.isEmpty()) throw new InvalidDisasterInput(field);
        try {
            return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new InvalidDisasterInput(field);
        }
    }
}
