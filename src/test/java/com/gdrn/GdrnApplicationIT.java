package com.gdrn;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GdrnApplicationIT {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @Autowired
    TestRestTemplate http;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void context_starts_with_flyway_and_postgis() {
        assertThat(jdbc.queryForObject("SELECT postgis_version()", String.class)).startsWith("3.5");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ST_Distance(ST_Point(0, 0), ST_Point(3, 4))",
                Double.class)).isEqualTo(5.0);
    }

    @Test
    void health_is_public_without_database_details() {
        var response = http.getForEntity("/actuator/health", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().has("components")).isFalse();
    }

    @Test
    void openapi_and_swagger_are_available_without_business_operations() {
        var response = http.getForEntity("/v3/api-docs", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("info").path("title").asText())
                .isEqualTo("Global Disaster Response Network API");
        assertThat(response.getBody().path("info").path("version").asText()).isEqualTo("Phase 1");
        assertThat(response.getBody().path("paths").size()).isZero();
        var swagger = http.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(swagger.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(swagger.getBody()).contains("Swagger UI");
    }

    @Test
    void unapproved_routes_and_writes_are_denied() {
        assertThat(http.getForEntity("/api/unimplemented", String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.getForEntity("/actuator/env", String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.postForEntity("/actuator/health", null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
