package com.gdrn.identity.application.port;

import com.gdrn.identity.domain.EmailAddress;
import com.gdrn.identity.domain.User;
import java.util.Optional;
import java.util.UUID;

public interface AccountStore {
    Optional<Account> findByEmail(EmailAddress email);
    Optional<User> findUser(UUID id);
    void createIfAbsent(User user, String passwordHash);

    record Account(User user, String passwordHash) {
        @Override public String toString() { return "Account[redacted]"; }
    }
}
