package com.gdrn.disaster.application;

public final class DisasterNotFound extends RuntimeException {
    public DisasterNotFound() { super("Disaster not found"); }
}
