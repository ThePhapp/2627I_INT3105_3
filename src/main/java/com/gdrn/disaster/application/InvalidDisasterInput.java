package com.gdrn.disaster.application;

public final class InvalidDisasterInput extends RuntimeException {
    private final String field;
    public InvalidDisasterInput(String field) { super("Invalid " + field); this.field = field; }
    public String field() { return field; }
}
