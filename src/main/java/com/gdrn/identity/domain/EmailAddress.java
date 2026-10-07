package com.gdrn.identity.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Normalized email address used as an identity value.
 */
public final class EmailAddress {
    private static final Pattern BASIC_EMAIL_FORMAT =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String value;

    private EmailAddress(String value) {
        this.value = value;
    }

    public static EmailAddress of(String rawValue) {
        Objects.requireNonNull(rawValue, "Email address must not be null");

        String normalizedValue = rawValue.trim().toLowerCase(Locale.ROOT);
        if (!BASIC_EMAIL_FORMAT.matcher(normalizedValue).matches()) {
            throw new IllegalArgumentException("Email address has an invalid format");
        }

        return new EmailAddress(normalizedValue);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EmailAddress that)) {
            return false;
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
