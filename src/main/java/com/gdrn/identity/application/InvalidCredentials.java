package com.gdrn.identity.application;

public final class InvalidCredentials extends RuntimeException {
    public InvalidCredentials() { super("Invalid email or password."); }
}
