package com.gdrn;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.context.ConfigurableApplicationContext;
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
import static org.awaitility.Awaitility.await;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.hikari.connection-timeout=1000",
        "spring.datasource.hikari.validation-timeout=500",
        "spring.datasource.hikari.data-source-properties.socketTimeout=2"
})
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

    @Autowired
    ConfigurableApplicationContext context;

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
    void probes_are_public_and_hide_components() {
        assertProbe("liveness", HttpStatus.OK, "UP");
        assertProbe("readiness", HttpStatus.OK, "UP");
    }

    @Test
    void refusing_traffic_changes_readiness_without_changing_liveness() {
        try {
            AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
            assertProbe("readiness", HttpStatus.SERVICE_UNAVAILABLE, "OUT_OF_SERVICE");
            assertProbe("liveness", HttpStatus.OK, "UP");
        } finally {
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }
        assertProbe("readiness", HttpStatus.OK, "UP");
    }

    @Test
    void database_outage_fails_readiness_but_does_not_fail_liveness() {
        // Pause only this test's disposable container, never the developer's Compose DB.
        var docker = DATABASE.getDockerClient();
        docker.pauseContainerCmd(DATABASE.getContainerId()).exec();
        try {
            assertProbe("readiness", HttpStatus.SERVICE_UNAVAILABLE, "DOWN");
            assertProbe("liveness", HttpStatus.OK, "UP");
        } finally {
            docker.unpauseContainerCmd(DATABASE.getContainerId()).exec();
        }
        await().atMost(Duration.ofSeconds(20)).untilAsserted(
                () -> assertProbe("readiness", HttpStatus.OK, "UP"));
    }

    private void assertProbe(String probe, HttpStatus status, String health) {
        var response = http.getForEntity("/actuator/health/" + probe, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody().path("status").asText()).isEqualTo(health);
        assertThat(response.getBody().has("components")).isFalse();
        assertThat(response.getBody().has("details")).isFalse();
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
        assertThat(http.getForEntity("/actuator/health/db", String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.postForEntity("/actuator/health", null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.postForEntity("/actuator/health/readiness", null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
