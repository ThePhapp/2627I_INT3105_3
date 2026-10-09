package com.gdrn.disaster.application;

import com.gdrn.disaster.application.port.DisasterIdGenerator;
import com.gdrn.disaster.application.port.DisasterRepository;
import com.gdrn.disaster.domain.*;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public final class DisasterService {
    private final DisasterRepository repository;
    private final DisasterIdGenerator ids;
    private final Clock clock;

    public DisasterService(DisasterRepository repository, DisasterIdGenerator ids, Clock clock) {
        this.repository = repository;
        this.ids = ids;
        this.clock = clock;
    }

    public Disaster create(Create command) {
        try {
            Disaster disaster = Disaster.create(ids.next(), command.name(), command.type(), command.severity(),
                    command.description(), command.latitude(), command.longitude(), clock.instant());
            repository.add(disaster);
            return disaster;
        } catch (InvalidDisaster error) {
            throw new InvalidDisasterInput(error.field());
        }
    }

    public Disaster get(UUID id) {
        return repository.findById(id).orElseThrow(DisasterNotFound::new);
    }

    public DisasterPage list(DisasterSearch search) {
        return repository.search(search);
    }

    public Disaster update(Update command) {
        Disaster current = get(command.id());
        Disaster changed;
        try {
            changed = current.update(command.expectedVersion(), command.change(), clock.instant());
        } catch (InvalidDisaster error) {
            throw new InvalidDisasterInput(error.field());
        }
        if (!repository.update(changed, command.expectedVersion())) throw new StaleDisasterVersion();
        return changed;
    }

    public record Create(String name, DisasterType type, Severity severity, String description,
                         double latitude, double longitude) {}
    public record Update(UUID id, long expectedVersion, Disaster.Change change) {
        public Update {
            if (id == null || change == null || expectedVersion < 0 || expectedVersion > 9_007_199_254_740_991L) {
                throw new InvalidDisasterInput(expectedVersion < 0 ? "expectedVersion" : "body");
            }
        }
    }

    public static Optional<String> text(String value) { return Optional.ofNullable(value); }
}
