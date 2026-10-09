package com.gdrn.identity.application;

import com.gdrn.identity.application.port.AccountStore;
import com.gdrn.identity.application.port.Passwords;
import com.gdrn.identity.domain.Role;
import com.gdrn.identity.domain.User;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class ProvisionDemoAccounts {
    private final AccountStore accounts;
    private final Passwords passwords;
    public ProvisionDemoAccounts(AccountStore accounts, Passwords passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }
    public void provision(List<Entry> entries) {
        if (entries.size() != 3 || entries.stream().filter(e -> e.role() == Role.CITIZEN).count() != 2
                || entries.stream().filter(e -> e.role() == Role.AUTHORITY).count() != 1) {
            throw new IllegalArgumentException("Demo requires two citizens and one authority");
        }
        var emails = new HashSet<String>();
        for (Entry entry : entries) {
            var email = IdentityInput.email(entry.email());
            IdentityInput.password(entry.password());
            if (!emails.add(email.value())) throw new IllegalArgumentException("Demo emails must be distinct");
        }
        for (Entry entry : entries) {
            var email = IdentityInput.email(entry.email());
            if (accounts.findByEmail(email).isEmpty()) {
                accounts.createIfAbsent(User.provision(UUID.randomUUID(), email, entry.role()), passwords.hash(entry.password()));
            }
        }
    }
    public record Entry(String email, String password, Role role) {
        @Override public String toString() { return "DemoEntry[redacted]"; }
    }
}
