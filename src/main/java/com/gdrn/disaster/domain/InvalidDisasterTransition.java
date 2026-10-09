package com.gdrn.disaster.domain;

public final class InvalidDisasterTransition extends RuntimeException {
    public InvalidDisasterTransition() {
        super("Disaster status transition is not allowed");
    }
}
