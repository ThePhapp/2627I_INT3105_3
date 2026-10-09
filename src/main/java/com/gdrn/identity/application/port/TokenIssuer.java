package com.gdrn.identity.application.port;

import com.gdrn.identity.domain.User;
import java.time.Instant;

public interface TokenIssuer {
    IssuedToken issue(User user);
    record IssuedToken(String value, Instant expiresAt, long expiresIn) {
        @Override public String toString() { return "IssuedToken[redacted]"; }
    }
}
