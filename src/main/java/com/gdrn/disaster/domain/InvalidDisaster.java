package com.gdrn.disaster.domain;

public final class InvalidDisaster extends RuntimeException {
    private final String field;

    public InvalidDisaster(String field) {
        super("Invalid disaster " + field);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
