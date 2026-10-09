package com.gdrn.identity.application;

import com.gdrn.identity.domain.EmailAddress;
import java.nio.charset.StandardCharsets;

public final class IdentityInput {
    private IdentityInput() {}
    public static EmailAddress email(String raw) {
        if (raw == null) throw new InvalidIdentityInput("email");
        try {
            EmailAddress email = EmailAddress.of(raw);
            if (email.value().codePointCount(0, email.value().length()) > 254) {
                throw new InvalidIdentityInput("email");
            }
            return email;
        } catch (IllegalArgumentException ex) {
            throw new InvalidIdentityInput("email");
        }
    }
    public static void password(String raw) {
        if (raw == null || raw.isEmpty() || raw.codePointCount(0, raw.length()) > 128
                || raw.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidIdentityInput("password");
        }
    }
}
