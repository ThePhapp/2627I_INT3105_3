package com.gdrn.identity.infrastructure.security;

import com.gdrn.identity.application.port.Passwords;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public final class EncodedPasswords implements Passwords {
    private final PasswordEncoder encoder;
    private final String dummyHash;
    public EncodedPasswords(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }
    public boolean matches(String raw, String hash) { return encoder.matches(raw, hash); }
    public String hash(String raw) { return encoder.encode(raw); }
    public void checkUnknownAccount(String raw) { encoder.matches(raw, dummyHash); }
}
