package com.gdrn.disaster.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Disaster {
    private final UUID id;
    private final String name;
    private final DisasterType type;
    private final Severity severity;
    private final String description;
    private final double latitude;
    private final double longitude;
    private final DisasterStatus status;
    private final long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Disaster(UUID id, String name, DisasterType type, Severity severity, String description,
                     double latitude, double longitude, DisasterStatus status, long version,
                     Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = text(name, 120, "name");
        this.type = required(type, "type");
        this.severity = required(severity, "severity");
        this.description = text(description, 2000, "description");
        this.latitude = coordinate(latitude, -90, 90, "latitude");
        this.longitude = coordinate(longitude, -180, 180, "longitude");
        this.status = required(status, "status");
        if (version < 0 || version > 9_007_199_254_740_991L) throw new InvalidDisaster("version");
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt)) throw new InvalidDisaster("updatedAt");
    }

    public static Disaster create(UUID id, String name, DisasterType type, Severity severity,
                                  String description, double latitude, double longitude, Instant now) {
        return new Disaster(id, name, type, severity, description, latitude, longitude,
                DisasterStatus.ACTIVE, 0, now, now);
    }

    public static Disaster reconstitute(UUID id, String name, DisasterType type, Severity severity,
                                        String description, double latitude, double longitude,
                                        DisasterStatus status, long version, Instant createdAt, Instant updatedAt) {
        return new Disaster(id, name, type, severity, description, latitude, longitude,
                status, version, createdAt, updatedAt);
    }

    public Disaster update(long expectedVersion, Change change, Instant now) {
        Objects.requireNonNull(change, "change");
        Objects.requireNonNull(now, "now");
        if (expectedVersion != version) throw new StaleDisasterVersion();
        if (status == DisasterStatus.RESOLVED) throw new InvalidDisasterTransition();
        if (change.isEmpty()) throw new InvalidDisaster("body");
        DisasterStatus nextStatus = change.status().orElse(status);
        if (change.status().isPresent() && nextStatus == status) throw new InvalidDisasterTransition();
        if (nextStatus != DisasterStatus.ACTIVE && nextStatus != DisasterStatus.RESOLVED) {
            throw new InvalidDisasterTransition();
        }
        return new Disaster(id, change.name().orElse(name), change.type().orElse(type),
                change.severity().orElse(severity), change.description().orElse(description),
                change.latitude().orElse(latitude), change.longitude().orElse(longitude),
                nextStatus, version + 1, createdAt, now);
    }

    private static String text(String value, int max, String field) {
        if (value == null) throw new InvalidDisaster(field);
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.codePointCount(0, normalized.length()) > max) {
            throw new InvalidDisaster(field);
        }
        return normalized;
    }

    private static double coordinate(double value, double min, double max, String field) {
        if (!Double.isFinite(value) || value < min || value > max) throw new InvalidDisaster(field);
        return value;
    }

    private static <T> T required(T value, String field) {
        if (value == null) throw new InvalidDisaster(field);
        return value;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public DisasterType type() { return type; }
    public Severity severity() { return severity; }
    public String description() { return description; }
    public double latitude() { return latitude; }
    public double longitude() { return longitude; }
    public DisasterStatus status() { return status; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public record Change(Optional<String> name, Optional<DisasterType> type,
                         Optional<Severity> severity, Optional<String> description,
                         Optional<Double> latitude, Optional<Double> longitude,
                         Optional<DisasterStatus> status) {
        public Change {
            Objects.requireNonNull(name); Objects.requireNonNull(type); Objects.requireNonNull(severity);
            Objects.requireNonNull(description); Objects.requireNonNull(latitude);
            Objects.requireNonNull(longitude); Objects.requireNonNull(status);
        }
        public boolean isEmpty() {
            return name.isEmpty() && type.isEmpty() && severity.isEmpty() && description.isEmpty()
                    && latitude.isEmpty() && longitude.isEmpty() && status.isEmpty();
        }
    }
}
