package com.gdrn.shared.infrastructure.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JsonConfiguration {
    @Bean Jackson2ObjectMapperBuilderCustomizer strictRequestJson() {
        return builder -> builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_TRAILING_TOKENS, JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .featuresToDisable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
    }
}
