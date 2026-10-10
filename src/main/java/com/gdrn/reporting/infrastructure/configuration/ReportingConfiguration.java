package com.gdrn.reporting.infrastructure.configuration;

import com.gdrn.reporting.application.ReportService;
import com.gdrn.reporting.application.ReportModerationService;
import com.gdrn.reporting.application.PublishedReportingQuery;
import com.gdrn.reporting.application.contract.ReportingQuery;
import com.gdrn.reporting.application.port.DisasterLookup;
import com.gdrn.reporting.application.port.ReportStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReportingConfiguration {
    @Bean ReportService reportService(ReportStore reports) {
        return new ReportService(reports, Clock.systemUTC());
    }
    @Bean ReportModerationService reportModerationService(ReportStore reports, DisasterLookup disasters) {
        return new ReportModerationService(reports, disasters, Clock.systemUTC());
    }
    @Bean ReportingQuery reportingQuery(ReportStore reports, DisasterLookup disasters) {
        return new PublishedReportingQuery(reports, disasters);
    }
}
