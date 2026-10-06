package com.gdrn.shared.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Global Disaster Response Network API")
                .description("Backend API for the Global Disaster Response Network.")
                .version("Phase 1"));
    }
}
