package com.gdrn.identity.application;

import com.gdrn.identity.application.port.*;
import com.gdrn.identity.domain.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentityServiceTest {
    private final AccountStore accounts = mock(AccountStore.class);
    private final Passwords passwords = mock(Passwords.class);
    private final TokenIssuer tokens = mock(TokenIssuer.class);
    private final IdentityService service = new IdentityService(accounts, passwords, tokens);

    @Test void normalizesEmailAndIssuesOnlyAfterPasswordCheck() {
        var user = User.register(UUID.randomUUID(), EmailAddress.of("one@example.test"));
        when(accounts.findByEmail(user.emailAddress())).thenReturn(Optional.of(new AccountStore.Account(user, "encoded")));
        when(passwords.matches("raw", "encoded")).thenReturn(true);
        when(tokens.issue(user)).thenReturn(new TokenIssuer.IssuedToken("opaque", Instant.now(), 900));
        assertThat(service.login(" ONE@example.test ", "raw").user()).isEqualTo(user);
        var order = inOrder(passwords, tokens);
        order.verify(passwords).matches("raw", "encoded");
        order.verify(tokens).issue(user);
    }

    @Test void missingAndUnsupportedAccountsNeverIssueTokens() {
        when(accounts.findByEmail(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login("none@example.test", "raw")).isInstanceOf(InvalidCredentials.class);
        verify(passwords).checkUnknownAccount("raw");
        var user = User.reconstitute(UUID.randomUUID(), EmailAddress.of("admin@example.test"), Role.ADMIN);
        when(accounts.findByEmail(any())).thenReturn(Optional.of(new AccountStore.Account(user, "encoded")));
        when(passwords.matches(any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.login("admin@example.test", "raw")).isInstanceOf(InvalidCredentials.class);
        verifyNoInteractions(tokens);
    }

    @Test void validatesByteLengthAndCurrentIdentity() {
        assertThatThrownBy(() -> service.login("one@example.test", "é".repeat(37))).isInstanceOf(InvalidIdentityInput.class);
        verifyNoInteractions(accounts);
        var id = UUID.randomUUID();
        when(accounts.findUser(id)).thenReturn(Optional.of(User.register(id, EmailAddress.of("one@example.test"))));
        assertThat(service.me(id, Role.CITIZEN).id()).isEqualTo(id);
        assertThatThrownBy(() -> service.me(id, Role.AUTHORITY)).isInstanceOf(InvalidCredentials.class);
    }

    @Test void provisioningValidatesAllEntriesBeforeWritingAndNeverResetsExistingAccount() {
        var provisioner = new ProvisionDemoAccounts(accounts, passwords);
        var entries = List.of(new ProvisionDemoAccounts.Entry("one@example.test", "raw", Role.CITIZEN),
                new ProvisionDemoAccounts.Entry("two@example.test", "raw", Role.CITIZEN),
                new ProvisionDemoAccounts.Entry("ops@example.test", "raw", Role.AUTHORITY));
        var user = User.register(UUID.randomUUID(), EmailAddress.of("one@example.test"));
        when(accounts.findByEmail(any())).thenReturn(Optional.of(new AccountStore.Account(user, "hash")));
        provisioner.provision(entries);
        verify(accounts, never()).createIfAbsent(any(), any());
        verifyNoInteractions(passwords);
        assertThatThrownBy(() -> provisioner.provision(List.of(entries.getFirst(), entries.getFirst(), entries.getLast())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
