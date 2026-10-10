package com.gdrn.reporting.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Incident lifecycle; withdrawal is distinct from verification status. */
public final class Report {
    private final UUID id;
    private final UUID reporterId;
    private final ReportType type;
    private final String description;
    private final Coordinates coordinates;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final ReportStatus status;
    private final UUID disasterId;
    private final Instant verifiedAt;
    private final String rejectionReason;
    private final Instant rejectedAt;
    private final Instant withdrawnAt;
    private final long version;

    private Report(UUID id, UUID reporterId, ReportType type, String description,
                   Coordinates coordinates, Instant createdAt, Instant updatedAt) {
        this(id, reporterId, type, description, coordinates, createdAt, updatedAt,
                ReportStatus.PENDING, null, null, null, null, null, 0);
    }

    private Report(UUID id, UUID reporterId, ReportType type, String description,
                   Coordinates coordinates, Instant createdAt, Instant updatedAt, ReportStatus status,
                   UUID disasterId, Instant verifiedAt, String rejectionReason, Instant rejectedAt,
                   Instant withdrawnAt, long version) {
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
        this.status = Objects.requireNonNull(status);
        this.disasterId = disasterId;
        this.verifiedAt = verifiedAt;
        this.rejectionReason = rejectionReason == null ? null : reason(rejectionReason);
        this.rejectedAt = rejectedAt;
        this.withdrawnAt = withdrawnAt;
        this.version = version;
        if (version < 0 || (withdrawnAt != null && status != ReportStatus.PENDING)) throw new InvalidReport("state");
        boolean valid = switch (status) {
            case PENDING -> disasterId == null && verifiedAt == null && rejectionReason == null && rejectedAt == null;
            case VERIFIED -> disasterId != null && verifiedAt != null && rejectionReason == null && rejectedAt == null;
            case REJECTED -> disasterId == null && verifiedAt == null && rejectionReason != null && rejectedAt != null;
        };
        if (!valid) throw new InvalidReport("state");
        for (Instant event : new Instant[]{verifiedAt, rejectedAt, withdrawnAt}) {
            if (event != null && (event.isBefore(createdAt) || !event.equals(updatedAt))) throw new InvalidReport("updatedAt");
        }
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
    public ReportStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public UUID disasterId() { return disasterId; }
    public Instant verifiedAt() { return verifiedAt; }
    public String rejectionReason() { return rejectionReason; }
    public Instant rejectedAt() { return rejectedAt; }
    public Instant withdrawnAt() { return withdrawnAt; }
    public long version() { return version; }

    public void requirePending() {
        if (status != ReportStatus.PENDING || withdrawnAt != null) throw new InvalidReportTransition();
    }

    public Report verify(UUID disaster, Instant now) {
        requirePending();
        if (disaster == null) throw new InvalidReport("disasterId");
        return changed(ReportStatus.VERIFIED, disaster, now, null, null, null, now);
    }

    public Report reject(String reason, Instant now) {
        requirePending();
        return changed(ReportStatus.REJECTED, null, null, reason(reason), now, null, now);
    }

    public Report withdraw(Instant now) {
        requirePending();
        return changed(status, null, null, null, null, now, now);
    }

    private Report changed(ReportStatus state, UUID disaster, Instant verified, String reason,
                           Instant rejected, Instant withdrawn, Instant now) {
        return reconstitute(id, reporterId, type, description, coordinates, createdAt, now, state,
                disaster, verified, reason, rejected, withdrawn, Math.incrementExact(version));
    }

    public static String reason(String raw) {
        if (raw == null) throw new InvalidReport("rejectionReason");
        String value = raw.strip();
        if (value.isBlank() || value.codePointCount(0, value.length()) > 500) throw new InvalidReport("rejectionReason");
        return value;
    }

    public static Report reconstitute(UUID id, UUID reporterId, ReportType type, String description,
            Coordinates coordinates, Instant createdAt, Instant updatedAt, ReportStatus status,
            UUID disasterId, Instant verifiedAt, String rejectionReason, Instant rejectedAt,
            Instant withdrawnAt, long version) {
        return new Report(id, reporterId, type, description, coordinates, createdAt, updatedAt, status,
                disasterId, verifiedAt, rejectionReason, rejectedAt, withdrawnAt, version);
    }
}
