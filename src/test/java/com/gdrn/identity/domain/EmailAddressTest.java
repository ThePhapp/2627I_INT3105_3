package com.gdrn.identity.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class EmailAddressTest {

    @Test
    void normalizesCaseAndSurroundingWhitespace() {
        EmailAddress emailAddress = EmailAddress.of("  User@Example.COM ");

        assertEquals("user@example.com", emailAddress.value());
    }

    @Test
    void rejectsAnInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("not-an-email"));
    }

    @Test
    void comparesByNormalizedValue() {
        assertEquals(EmailAddress.of("user@example.com"), EmailAddress.of("USER@EXAMPLE.COM"));
    }
}
