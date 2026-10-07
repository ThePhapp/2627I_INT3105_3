package com.gdrn.identity.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity aggregate for a registered user.
 */
public final class User {
    private final UUID id;
    private final EmailAddress emailAddress;
    private final Role role;

    private User(UUID id, EmailAddress emailAddress, Role role) {
        this.id = Objects.requireNonNull(id, "User id must not be null");
        this.emailAddress = Objects.requireNonNull(emailAddress, "Email address must not be null");
        this.role = Objects.requireNonNull(role, "User role must not be null");
    }

    public static User register(UUID id, EmailAddress emailAddress) {
        return new User(id, emailAddress, Role.CITIZEN);
    }

    public UUID id() {
        return id;
    }

    public EmailAddress emailAddress() {
        return emailAddress;
    }

    public Role role() {
        return role;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
