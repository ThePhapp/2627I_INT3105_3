package com.gdrn.identity.infrastructure;

import com.gdrn.identity.infrastructure.security.JwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class JwtConfigurationTest {
    @Test void missingAndWeakKeysFailClosedWithoutEchoingValue() {
        var runner = new ApplicationContextRunner().withUserConfiguration(JwtConfiguration.class);
        runner.run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("JWT_SECRET_BASE64=invalid-input-value")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("JWT_SECRET_BASE64 must encode at least 32 random bytes");
                    var trace = new java.io.StringWriter();
                    context.getStartupFailure().printStackTrace(new java.io.PrintWriter(trace));
                    assertThat(trace.toString()).doesNotContain("invalid-input-value");
                });
    }
}
