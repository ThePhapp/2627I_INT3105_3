package com.gdrn.reporting.domain;

public final class InvalidReport extends IllegalArgumentException {
    private final String field;

    public InvalidReport(String field) {
        super("Invalid report " + field + ".");
        this.field = field;
    }

    public String field() { return field; }
}
