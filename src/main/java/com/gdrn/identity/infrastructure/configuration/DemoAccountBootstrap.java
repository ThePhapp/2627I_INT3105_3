package com.gdrn.identity.infrastructure.configuration;

import com.gdrn.identity.application.ProvisionDemoAccounts;
import com.gdrn.identity.domain.Role;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;

@Component
@Profile("demo")
public final class DemoAccountBootstrap implements ApplicationRunner {
    private final Environment environment;
    private final ProvisionDemoAccounts provisioner;
    private final TransactionTemplate transaction;
    public DemoAccountBootstrap(Environment environment, ProvisionDemoAccounts provisioner, TransactionTemplate transaction) {
        this.environment = environment;
        this.provisioner = provisioner;
        this.transaction = transaction;
    }
    public void run(ApplicationArguments arguments) {
        var entries = List.of(entry("DEMO_CITIZEN_ONE", Role.CITIZEN),
                entry("DEMO_CITIZEN_TWO", Role.CITIZEN), entry("DEMO_AUTHORITY", Role.AUTHORITY));
        try {
            transaction.executeWithoutResult(status -> provisioner.provision(entries));
        } catch (IllegalArgumentException | com.gdrn.identity.application.InvalidIdentityInput ex) {
            throw new IllegalStateException("Invalid demo account configuration; check email/password limits and distinct emails.");
        }
    }
    private ProvisionDemoAccounts.Entry entry(String prefix, Role role) {
        return new ProvisionDemoAccounts.Entry(required(prefix + "_EMAIL"), required(prefix + "_PASSWORD"), role);
    }
    private String required(String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Demo profile requires " + name);
        return value;
    }
}
