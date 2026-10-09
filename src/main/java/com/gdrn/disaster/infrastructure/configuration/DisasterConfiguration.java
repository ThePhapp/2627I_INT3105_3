package com.gdrn.disaster.infrastructure.configuration;

import com.gdrn.disaster.application.DisasterService;
import com.gdrn.disaster.application.PublishedDisasterQuery;
import com.gdrn.disaster.application.contract.DisasterQuery;
import com.gdrn.disaster.application.port.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class DisasterConfiguration {
    @Bean DisasterIdGenerator disasterIdGenerator() { return UUID::randomUUID; }
    @Bean DisasterService disasterService(DisasterRepository repository, DisasterIdGenerator ids, Clock clock) {
        return new DisasterService(repository, ids, clock);
    }
    @Bean DisasterQuery disasterQuery(DisasterRepository repository) {
        return new PublishedDisasterQuery(repository);
    }
}
