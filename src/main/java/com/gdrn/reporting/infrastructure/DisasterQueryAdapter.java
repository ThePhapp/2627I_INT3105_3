package com.gdrn.reporting.infrastructure;

import com.gdrn.disaster.application.contract.DisasterQuery;
import com.gdrn.reporting.application.contract.LinkedDisaster;
import com.gdrn.reporting.application.contract.LinkedDisasterState;
import com.gdrn.reporting.application.port.DisasterLookup;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DisasterQueryAdapter implements DisasterLookup {
    private final DisasterQuery query;
    public DisasterQueryAdapter(DisasterQuery query) { this.query = query; }
    @Override public Map<UUID, LinkedDisaster> findByIds(Set<UUID> ids) {
        var result = new HashMap<UUID, LinkedDisaster>();
        query.findByIds(ids).forEach((id, snapshot) -> result.put(id, new LinkedDisaster(snapshot.id(), switch (snapshot.status()) {
            case ACTIVE -> LinkedDisasterState.ACTIVE;
            case RESOLVED -> LinkedDisasterState.RESOLVED;
        })));
        return Map.copyOf(result);
    }
}
