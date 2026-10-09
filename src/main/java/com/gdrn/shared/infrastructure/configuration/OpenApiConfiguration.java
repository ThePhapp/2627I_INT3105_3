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
    OpenApiCustomizer implementedContracts() throws IOException {
        // Only implemented slices are shipped; never advertise planned operations.
        OpenAPI identity = read("openapi/identity.json");
        OpenAPI disaster = read("openapi/disaster.json");
        return api -> {
            for (OpenAPI contract : java.util.List.of(identity, disaster)) {
                contract.getPaths().forEach(api::path);
                contract.getComponents().getSchemas().forEach(api.getComponents()::addSchemas);
                if (contract.getComponents().getSecuritySchemes() != null) {
                    contract.getComponents().getSecuritySchemes().forEach(api.getComponents()::addSecuritySchemes);
                }
            }
        };
    }
    private static OpenAPI read(String path) throws IOException {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return Json.mapper().readValue(input, OpenAPI.class);
        }
    }
    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Global Disaster Response Network API")
                .description("Backend API for the Global Disaster Response Network.")
                .version("Phase 1"));
    }
}
