package com.gdrn.identity.application;

public final class InvalidIdentityInput extends RuntimeException {
    private final String field;
    public InvalidIdentityInput(String field) {
        super("Invalid " + field + ".");
        this.field = field;
    }
    public String field() { return field; }
}
