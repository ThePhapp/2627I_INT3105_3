package com.gdrn.identity.application;

import com.gdrn.identity.application.port.AccountStore;
import com.gdrn.identity.application.port.Passwords;
import com.gdrn.identity.application.port.TokenIssuer;
import com.gdrn.identity.domain.Role;
import com.gdrn.identity.domain.User;
import java.util.UUID;

public final class IdentityService {
    private final AccountStore accounts;
    private final Passwords passwords;
    private final TokenIssuer tokens;

    public IdentityService(AccountStore accounts, Passwords passwords, TokenIssuer tokens) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public LoginResult login(String rawEmail, String password) {
        var email = IdentityInput.email(rawEmail);
        IdentityInput.password(password);
        var account = accounts.findByEmail(email);
        if (account.isEmpty()) {
            passwords.checkUnknownAccount(password);
            throw new InvalidCredentials();
        }
        var found = account.get();
        boolean matches = passwords.matches(password, found.passwordHash());
        if (!matches || !found.user().canSignIn()) throw new InvalidCredentials();
        return new LoginResult(found.user(), tokens.issue(found.user()));
    }

    public User me(UUID authenticatedId, Role authenticatedRole) {
        return accounts.findUser(authenticatedId)
                .filter(User::canSignIn)
                .filter(user -> user.role() == authenticatedRole)
                .orElseThrow(InvalidCredentials::new);
    }

    public record LoginResult(User user, TokenIssuer.IssuedToken token) {}
}
