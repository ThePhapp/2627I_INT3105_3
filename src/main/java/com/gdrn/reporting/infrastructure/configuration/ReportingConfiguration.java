package com.gdrn.reporting.infrastructure.configuration;

import com.gdrn.reporting.application.ReportService;
import com.gdrn.reporting.application.port.ReportStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReportingConfiguration {
    @Bean ReportService reportService(ReportStore reports) {
        return new ReportService(reports, Clock.systemUTC());
    }
}
