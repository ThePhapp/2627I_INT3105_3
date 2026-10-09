package com.gdrn.identity.domain;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class UserTest {
    @Test
    void provisioningAndReconstitutionPreserveRoleWithoutExpandingMvpAccess() {
        var email = EmailAddress.of("authority@example.test");
        assertEquals(Role.AUTHORITY, User.provision(UUID.randomUUID(), email, Role.AUTHORITY).role());
        assertThrows(IllegalArgumentException.class, () -> User.provision(UUID.randomUUID(), email, Role.ADMIN));
        assertEquals(false, User.reconstitute(UUID.randomUUID(), email, Role.RESPONDER).canSignIn());
    }

    @Test
    void registrationAssignsTheCitizenRoleByDefault() {
        UUID id = UUID.randomUUID();
        EmailAddress emailAddress = EmailAddress.of("citizen@example.com");

        User user = User.register(id, emailAddress);

        assertEquals(id, user.id());
        assertEquals(emailAddress, user.emailAddress());
        assertEquals(Role.CITIZEN, user.role());
    }

    @Test
    void rejectsMissingIdentityData() {
        EmailAddress emailAddress = EmailAddress.of("citizen@example.com");

        assertThrows(NullPointerException.class, () -> User.register(null, emailAddress));
        assertThrows(NullPointerException.class, () -> User.register(UUID.randomUUID(), null));
    }

    @Test
    void comparesUsersByIdentity() {
        UUID id = UUID.randomUUID();

        User first = User.register(id, EmailAddress.of("first@example.com"));
        User second = User.register(id, EmailAddress.of("second@example.com"));

        assertEquals(first, second);
    }
}
