package com.gdrn.reporting.application.port;

import com.gdrn.reporting.application.contract.LinkedDisaster;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Consumer-owned port. The outer adapter calls the published Disaster query. */
public interface DisasterLookup {
    Map<UUID, LinkedDisaster> findByIds(Set<UUID> ids);
}
