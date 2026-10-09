package com.gdrn.identity.infrastructure.configuration;

import com.gdrn.identity.application.IdentityService;
import com.gdrn.identity.application.ProvisionDemoAccounts;
import com.gdrn.identity.application.port.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class IdentityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }
    @Bean IdentityService identityService(AccountStore accounts, Passwords passwords, TokenIssuer tokens) {
        return new IdentityService(accounts, passwords, tokens);
    }
    @Bean ProvisionDemoAccounts provisionDemoAccounts(AccountStore accounts, Passwords passwords) {
        return new ProvisionDemoAccounts(accounts, passwords);
    }
}
