package com.gdrn.reporting.domain;

public record Coordinates(double latitude, double longitude) {
    public Coordinates {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new InvalidReport("latitude");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new InvalidReport("longitude");
        }
    }
}
