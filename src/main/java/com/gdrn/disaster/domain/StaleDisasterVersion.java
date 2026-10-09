package com.gdrn.disaster.domain;

public final class StaleDisasterVersion extends RuntimeException {
    public StaleDisasterVersion() {
        super("Disaster version is stale");
    }
}
