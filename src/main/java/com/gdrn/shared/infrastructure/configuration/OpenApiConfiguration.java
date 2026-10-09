package com.gdrn.shared.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.io.ClassPathResource;
import io.swagger.v3.core.util.Json;
import java.io.IOException;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    OpenApiCustomizer identityContract() throws IOException {
        // Only the implemented slice is shipped, never advertise the other 13 planned operations.
        OpenAPI contract;
        try (var input = new ClassPathResource("openapi/identity.json").getInputStream()) {
            contract = Json.mapper().readValue(input, OpenAPI.class);
        }
        return api -> {
            contract.getPaths().forEach(api::path);
            contract.getComponents().getSchemas().forEach(api.getComponents()::addSchemas);
            contract.getComponents().getSecuritySchemes().forEach(api.getComponents()::addSecuritySchemes);
        };
    }
    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Global Disaster Response Network API")
                .description("Backend API for the Global Disaster Response Network.")
                .version("Phase 1"));
    }
}
