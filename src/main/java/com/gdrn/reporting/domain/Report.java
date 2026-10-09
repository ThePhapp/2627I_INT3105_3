package com.gdrn.reporting.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A submitted incident. Verification and withdrawal commands belong to B2. */
public final class Report {
    private final UUID id;
    private final UUID reporterId;
    private final ReportType type;
    private final String description;
    private final Coordinates coordinates;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Report(UUID id, UUID reporterId, ReportType type, String description,
                   Coordinates coordinates, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.reporterId = Objects.requireNonNull(reporterId);
        if (type == null) throw new InvalidReport("type");
        this.type = type;
        if (description == null) throw new InvalidReport("description");
        String text = description.strip();
        if (text.isBlank() || text.codePointCount(0, text.length()) > 2000) {
            throw new InvalidReport("description");
        }
        this.description = text;
        this.coordinates = Objects.requireNonNull(coordinates);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        if (updatedAt.isBefore(createdAt)) throw new InvalidReport("updatedAt");
    }

    public static Report submit(UUID id, UUID reporterId, ReportType type, String description,
                                Coordinates coordinates, Instant now) {
        return new Report(id, reporterId, type, description, coordinates, now, now);
    }

    public static Report reconstitutePending(UUID id, UUID reporterId, ReportType type, String description,
                                             Coordinates coordinates, Instant createdAt, Instant updatedAt) {
        return new Report(id, reporterId, type, description, coordinates, createdAt, updatedAt);
    }

    public UUID id() { return id; }
    public UUID reporterId() { return reporterId; }
    public ReportType type() { return type; }
    public String description() { return description; }
    public Coordinates coordinates() { return coordinates; }
    public ReportStatus status() { return ReportStatus.PENDING; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
