package com.gdrn.identity.application.port;

public interface Passwords {
    boolean matches(String raw, String hash);
    String hash(String raw);
    void checkUnknownAccount(String raw);
}
