package com.gdrn.disaster.infrastructure.persistence;

import com.gdrn.disaster.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disasters")
public class DisasterEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DisasterType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Severity severity;
    @Column(nullable = false, length = 2000) private String description;
    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DisasterStatus status;
    @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected DisasterEntity() {}

    private DisasterEntity(Disaster disaster) {
        this.id = disaster.id(); this.name = disaster.name(); this.type = disaster.type();
        this.severity = disaster.severity(); this.description = disaster.description();
        this.latitude = disaster.latitude(); this.longitude = disaster.longitude();
        this.status = disaster.status(); this.version = disaster.version();
        this.createdAt = disaster.createdAt(); this.updatedAt = disaster.updatedAt();
    }

    static DisasterEntity from(Disaster disaster) { return new DisasterEntity(disaster); }
    Disaster toDomain() {
        return Disaster.reconstitute(id, name, type, severity, description, latitude, longitude,
                status, version, createdAt, updatedAt);
    }
}
