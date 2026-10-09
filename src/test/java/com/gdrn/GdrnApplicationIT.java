package com.gdrn;

import java.time.Duration;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ConfigurableApplicationContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import com.gdrn.identity.application.port.AccountStore;
import com.gdrn.identity.domain.*;
import com.gdrn.identity.infrastructure.configuration.DemoAccountBootstrap;
import com.gdrn.disaster.application.*;
import com.gdrn.disaster.application.contract.*;
import com.gdrn.disaster.application.port.DisasterRepository;
import com.gdrn.disaster.domain.*;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.time.Instant;
import java.util.*;
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
@ActiveProfiles({"test", "demo"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.hikari.connection-timeout=1000",
        "spring.datasource.hikari.validation-timeout=500",
        "spring.datasource.hikari.data-source-properties.socketTimeout=2"
})
class GdrnApplicationIT {
    static final String SECRET = randomKey();
    static final String PASSWORD = UUID.randomUUID().toString();
    static String randomKey() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
        registry.add("JWT_SECRET_BASE64", () -> SECRET);
        registry.add("DEMO_CITIZEN_ONE_EMAIL", () -> "one@example.test");
        registry.add("DEMO_CITIZEN_TWO_EMAIL", () -> "two@example.test");
        registry.add("DEMO_AUTHORITY_EMAIL", () -> "ops@example.test");
        registry.add("DEMO_CITIZEN_ONE_PASSWORD", () -> PASSWORD);
        registry.add("DEMO_CITIZEN_TWO_PASSWORD", () -> PASSWORD);
        registry.add("DEMO_AUTHORITY_PASSWORD", () -> PASSWORD);
    }

    @Autowired
    TestRestTemplate http;

    @Autowired
    JdbcTemplate jdbc;
    @Autowired JwtEncoder encoder;
    @Autowired AccountStore accounts;
    @Autowired DemoAccountBootstrap bootstrap;
    @Autowired ConfigurableEnvironment environment;
    @Autowired ConfigurableApplicationContext context;
    @Autowired DisasterRepository disasters;
    @Autowired DisasterQuery disasterQuery;

    @Test
    void context_starts_with_flyway_and_postgis() {
        assertThat(jdbc.queryForObject("SELECT postgis_version()", String.class)).startsWith("3.5");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '2' AND success", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '3' AND success", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '4' AND success", Integer.class)).isEqualTo(1);
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

    @Test void migrationUpgradesV2ToV3WithoutProvisioningAccountsOrDisasters() {
        var base = org.flywaydb.core.Flyway.configure().dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
                .schemas("disaster_upgrade_check").target("2").load();
        base.migrate();
        var upgrade = org.flywaydb.core.Flyway.configure().dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
                .schemas("disaster_upgrade_check").target("3").load();
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from disaster_upgrade_check.identity_users", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from disaster_upgrade_check.identity_credentials", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from disaster_upgrade_check.disasters", Integer.class)).isZero();
    }

    @Test void disasterAdapterPersistsFiltersPagesPublishesAndRejectsStaleUpdate() {
        Instant now = Instant.parse("2026-10-09T02:00:00Z");
        UUID activeId = UUID.randomUUID();
        UUID resolvedId = UUID.randomUUID();
        disasters.add(Disaster.create(activeId, "Active flood", DisasterType.FLOOD, Severity.HIGH,
                "Training active", 21.028, 105.834, now));
        var resolved = Disaster.create(resolvedId, "Resolved fire", DisasterType.WILDFIRE, Severity.MODERATE,
                "Training resolved", 10, 106, now.plusSeconds(1)).update(0,
                new Disaster.Change(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.of(DisasterStatus.RESOLVED)), now.plusSeconds(2));
        disasters.add(resolved);

        var page = disasters.search(new DisasterSearch(0, 1, DisasterSearch.Sort.CREATED_AT_DESC,
                Optional.empty(), Optional.empty()));
        assertThat(page.items()).extracting(Disaster::id).containsExactly(resolvedId);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(disasters.search(new DisasterSearch(0, 20, DisasterSearch.Sort.CREATED_AT_ASC,
                Optional.of(DisasterStatus.ACTIVE), Optional.of(DisasterType.FLOOD))).items())
                .extracting(Disaster::id).containsExactly(activeId);
        assertThat(disasterQuery.findByIds(Set.of(activeId, resolvedId, UUID.randomUUID())))
                .containsOnlyKeys(activeId, resolvedId);
        assertThat(disasterQuery.findById(resolvedId).orElseThrow().status()).isEqualTo(DisasterState.RESOLVED);

        var current = disasters.findById(activeId).orElseThrow();
        var changed = current.update(0, new Disaster.Change(Optional.of("Updated flood"), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), now.plusSeconds(3));
        assertThat(disasters.update(changed, 0)).isTrue();
        assertThat(disasters.update(changed, 0)).isFalse();
        assertThat(disasters.findById(activeId).orElseThrow().name()).isEqualTo("Updated flood");
    }

    @Test
    void probes_are_public_and_hide_components() {
        assertProbe("liveness", HttpStatus.OK, "UP");
        assertProbe("readiness", HttpStatus.OK, "UP");
    }

    @Test void disasterPaginationUsesOneSnapshotWhenAnotherTransactionInsertsBetweenItemsAndCount() throws Exception {
        // Interpose only at the adapter's SELECT boundary; both SELECTs and the concurrent write hit real PostGIS.
        Object target = org.springframework.test.util.AopTestUtils.getTargetObject(disasters);
        var original = (jakarta.persistence.EntityManager) org.springframework.test.util.ReflectionTestUtils.getField(target, "entityManager");
        var intercepted = org.mockito.Mockito.spy(original);
        UUID id = UUID.randomUUID();
        long before = disasters.search(new DisasterSearch(0, 100, DisasterSearch.Sort.CREATED_AT_DESC,
                Optional.empty(), Optional.empty())).totalElements();
        org.mockito.Mockito.doAnswer(invocation -> {
            var query = (jakarta.persistence.TypedQuery<?>) invocation.callRealMethod();
            var querySpy = org.mockito.Mockito.spy(query);
            org.mockito.Mockito.doAnswer(read -> {
                Object items = read.callRealMethod();
                try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
                    worker.submit(() -> disasters.add(Disaster.create(id, "Concurrent snapshot", DisasterType.TSUNAMI,
                            Severity.LOW, "Snapshot fixture", 0, 0, Instant.now())))
                            .get(10, java.util.concurrent.TimeUnit.SECONDS);
                }
                return items;
            }).when(querySpy).getResultList();
            return querySpy;
        }).when(intercepted).createQuery(org.mockito.ArgumentMatchers.startsWith("select d from DisasterEntity d"),
                org.mockito.ArgumentMatchers.eq(com.gdrn.disaster.infrastructure.persistence.DisasterEntity.class));
        try {
            org.springframework.test.util.ReflectionTestUtils.setField(target, "entityManager", intercepted);
            var page = disasters.search(new DisasterSearch(0, 100, DisasterSearch.Sort.CREATED_AT_DESC,
                    Optional.empty(), Optional.empty()));
            assertThat(page.totalElements()).isEqualTo(before);
            assertThat(page.items()).extracting(Disaster::id).doesNotContain(id);
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(target, "entityManager", original);
            jdbc.update("delete from disasters where id = ?", id);
        }
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
    void openapi_and_swagger_publish_exactly_the_implemented_slices() {
        var response = http.getForEntity("/v3/api-docs", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("info").path("title").asText())
                .isEqualTo("Global Disaster Response Network API");
        assertThat(response.getBody().path("info").path("version").asText()).isEqualTo("Phase 1");
        assertThat(response.getBody().path("paths").size()).isEqualTo(6);
        assertThat(response.getBody().at("/paths/~1api~1auth~1login/post/operationId").asText()).isEqualTo("E01");
        assertThat(response.getBody().at("/paths/~1api~1auth~1me/get/operationId").asText()).isEqualTo("E02");
        assertThat(response.getBody().at("/paths/~1api~1disasters/get/operationId").asText()).isEqualTo("E03");
        assertThat(response.getBody().at("/paths/~1api~1disasters~1{id}/get/operationId").asText()).isEqualTo("E04");
        assertThat(response.getBody().at("/paths/~1api~1disasters/post/operationId").asText()).isEqualTo("E05");
        assertThat(response.getBody().at("/paths/~1api~1disasters~1{id}/patch/operationId").asText()).isEqualTo("E06");
        assertThat(response.getBody().at("/paths/~1api~1reports/post/operationId").asText()).isEqualTo("E07");
        assertThat(response.getBody().at("/paths/~1api~1reports/get/operationId").asText()).isEqualTo("E08");
        assertThat(response.getBody().at("/paths/~1api~1reports~1{id}/get/operationId").asText()).isEqualTo("E09");
        Set<String> operations = new HashSet<>();
        response.getBody().path("paths").forEach(path -> path.forEach(operation -> {
            if (operation.has("operationId")) operations.add(operation.path("operationId").asText());
        }));
        assertThat(operations).containsExactlyInAnyOrder("E01", "E02", "E03", "E04", "E05", "E06", "E07", "E08", "E09");
        assertThat(response.getBody().at("/paths/~1api~1reports/post/security/0/bearerAuth").isArray()).isTrue();
        assertThat(response.getBody().at("/paths/~1api~1reports/get/parameters").toString()).doesNotContain("radiusMeters", "ownerId");
        assertThat(response.getBody().at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(response.getBody().at("/paths/~1api~1auth~1me/get/security/0/bearerAuth").isArray()).isTrue();
        var swagger = http.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(swagger.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(swagger.getBody()).contains("Swagger UI");
    }

    @Test
    void unapproved_routes_and_writes_are_denied() {
        assertThat(http.getForEntity("/api/unimplemented", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.getForEntity("/actuator/env", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.postForEntity("/actuator/health", null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.getForEntity("/actuator/health/db", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.postForEntity("/actuator/health/readiness", null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        String token = login("one@example.test").path("accessToken").asText();
        assertError(call(HttpMethod.GET, "/actuator/health/db", null, token), 403, "FORBIDDEN");
        for (String path : List.of("/api/rescue-missions", "/api/auth/register", "/api/auth/refresh", "/api/auth/logout")) {
            assertError(call(HttpMethod.GET, path, null, token), 403, "FORBIDDEN");
            assertError(call(HttpMethod.POST, path, "{}", token), 403, "FORBIDDEN");
        }
    }

    @Test void disasterApisEnforceRolesExpectedVersionAndLifecycle() {
        String citizen = login("one@example.test").path("accessToken").asText();
        String authority = login("ops@example.test").path("accessToken").asText();
        assertError(call(HttpMethod.GET, "/api/disasters", null, null), 401, "UNAUTHENTICATED");
        assertThat(call(HttpMethod.GET, "/api/disasters", null, citizen).getStatusCode().value()).isEqualTo(200);

        String create = """
                {"name":"  C1 flood exercise  ","type":"FLOOD","severity":"HIGH",
                 "description":"  Water is rising  ","latitude":21.028,"longitude":105.834}
                """;
        assertError(call(HttpMethod.POST, "/api/disasters", create, citizen), 403, "FORBIDDEN");
        assertError(call(HttpMethod.PATCH, "/api/disasters/00000000-0000-4000-8000-000000000004",
                "{\"expectedVersion\":0,\"status\":\"RESOLVED\"}", citizen), 403, "FORBIDDEN");

        var created = call(HttpMethod.POST, "/api/disasters", create, authority);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(created.getBody().path("name").asText()).isEqualTo("C1 flood exercise");
        assertThat(created.getBody().path("status").asText()).isEqualTo("ACTIVE");
        assertThat(created.getBody().path("version").asLong()).isZero();
        String path = "/api/disasters/" + created.getBody().path("id").asText();
        assertThat(call(HttpMethod.GET, path, null, citizen).getBody()).isEqualTo(created.getBody());
        assertThat(call(HttpMethod.GET, path, null, authority).getStatusCode().value()).isEqualTo(200);

        var edited = call(HttpMethod.PATCH, path,
                "{\"expectedVersion\":0,\"severity\":\"CRITICAL\"}", authority);
        assertThat(edited.getStatusCode().value()).isEqualTo(200);
        assertThat(edited.getBody().path("version").asLong()).isEqualTo(1);
        assertThat(edited.getBody().path("severity").asText()).isEqualTo("CRITICAL");
        assertError(call(HttpMethod.PATCH, path,
                "{\"expectedVersion\":0,\"name\":\"Stale\"}", authority), 409, "STALE_VERSION");

        var resolved = call(HttpMethod.PATCH, path,
                "{\"expectedVersion\":1,\"status\":\"RESOLVED\"}", authority);
        assertThat(resolved.getStatusCode().value()).isEqualTo(200);
        assertThat(resolved.getBody().path("status").asText()).isEqualTo("RESOLVED");
        assertError(call(HttpMethod.PATCH, path,
                "{\"expectedVersion\":2,\"name\":\"Cannot change\"}", authority), 409, "INVALID_TRANSITION");
    }

    @Test void disasterApisValidateShapeReferencesFiltersAndSort() {
        String authority = login("ops@example.test").path("accessToken").asText();
        for (String body : List.of("{}", "null", "[]", "{", "{\"name\":null}",
                "{\"name\":\"x\",\"type\":\"flood\",\"severity\":\"HIGH\",\"description\":\"x\",\"latitude\":0,\"longitude\":0}",
                "{\"name\":\"x\",\"type\":\"FLOOD\",\"severity\":\"HIGH\",\"description\":\"x\",\"latitude\":91,\"longitude\":0}",
                "{\"name\":\"x\",\"type\":\"FLOOD\",\"severity\":\"HIGH\",\"description\":\"x\",\"latitude\":0,\"longitude\":0,\"extra\":true}")) {
            assertError(call(HttpMethod.POST, "/api/disasters", body, authority), 400, "VALIDATION_ERROR");
        }
        var textHeaders = new HttpHeaders(); textHeaders.setContentType(MediaType.TEXT_PLAIN); textHeaders.setBearerAuth(authority);
        assertError(http.exchange("/api/disasters", HttpMethod.POST, new HttpEntity<>("data", textHeaders), JsonNode.class),
                415, "UNSUPPORTED_MEDIA_TYPE");
        for (String path : List.of("/api/disasters?page=-1", "/api/disasters?size=101",
                "/api/disasters?sort=name,asc", "/api/disasters?status=active", "/api/disasters?unknown=1",
                "/api/disasters?page=0&page=1")) {
            assertError(call(HttpMethod.GET, path, null, authority), 400, "VALIDATION_ERROR");
        }
        assertError(call(HttpMethod.GET, "/api/disasters/NOT-A-UUID", null, authority), 400, "VALIDATION_ERROR");
        assertError(call(HttpMethod.GET, "/api/disasters/00000000-0000-4000-8000-000000000099", null, authority),
                404, "NOT_FOUND");
        for (String body : List.of("{}", "null", "[]", "{\"expectedVersion\":0}",
                "{\"expectedVersion\":null,\"name\":\"x\"}",
                "{\"expectedVersion\":0,\"status\":\"ACTIVE\"}",
                "{\"expectedVersion\":0,\"name\":null}")) {
            var response = call(HttpMethod.PATCH, "/api/disasters/00000000-0000-4000-8000-000000000099", body, authority);
            if (body.contains("\"status\":\"ACTIVE\"")) assertError(response, 404, "NOT_FOUND");
            else assertError(response, 400, "VALIDATION_ERROR");
        }
        var filtered = call(HttpMethod.GET,
                "/api/disasters?page=0&size=100&sort=createdAt,asc&status=ACTIVE&type=FLOOD", null, authority);
        assertThat(filtered.getStatusCode().value()).isEqualTo(200);
        assertThat(filtered.getBody().path("items")).allSatisfy(item -> {
            assertThat(item.path("status").asText()).isEqualTo("ACTIVE");
            assertThat(item.path("type").asText()).isEqualTo("FLOOD");
        });
    }

    @Test void loginAndMeUseRealCredentialsForBothRolesWithoutLeakingHashOrPassword() {
        assertThat(call(HttpMethod.POST, "/api/auth/login", "{\"email\":\"one@example.test\",\"password\":\""+PASSWORD+"\"}", "expired-or-malformed").getStatusCode().value()).isEqualTo(200);
        Set<String> ids = new HashSet<>();
        for (String email : List.of("one@example.test", "two@example.test", "ops@example.test")) {
            JsonNode loggedIn = login(email.toUpperCase(Locale.ROOT));
            assertThat(loggedIn.path("expiresIn").asLong()).isEqualTo(900);
            assertThat(loggedIn.path("tokenType").asText()).isEqualTo("Bearer");
            assertThat(loggedIn.toString()).doesNotContain("passwordHash", PASSWORD);
            var me = call(HttpMethod.GET, "/api/auth/me", null, loggedIn.path("accessToken").asText());
            assertThat(me.getStatusCode().value()).isEqualTo(200);
            assertThat(me.getHeaders().getCacheControl()).isEqualTo("no-store");
            assertThat(me.getHeaders().getFirst("Set-Cookie")).isNull();
            assertThat(me.getBody()).isEqualTo(loggedIn.path("user"));
            assertThat(me.getBody().path("role").asText()).isEqualTo(email.startsWith("ops") ? "AUTHORITY" : "CITIZEN");
            ids.add(me.getBody().path("id").asText());
        }
        assertThat(ids).hasSize(3);
    }

    @Test void wrongPasswordUnknownUserAndUnsupportedRoleHaveSameLoginFailure() {
        String wrong = "{\"email\":\"one@example.test\",\"password\":\"wrong\"}";
        var response = call(HttpMethod.POST, "/api/auth/login", wrong, null);
        assertError(response, 401, "INVALID_CREDENTIALS");
        var unknown = call(HttpMethod.POST, "/api/auth/login", wrong.replace("one@", "unknown@"), null);
        assertError(unknown, 401, "INVALID_CREDENTIALS");
        assertThat(response.getBody().path("message")).isEqualTo(unknown.getBody().path("message"));
        var hash = jdbc.queryForObject("select password_hash from identity_credentials c join identity_users u on c.user_id=u.id where u.email='one@example.test'", String.class);
        for (Role role : List.of(Role.ADMIN, Role.RESPONDER)) {
            String email = role.name().toLowerCase(Locale.ROOT) + "@example.test";
            accounts.createIfAbsent(User.reconstitute(UUID.randomUUID(), EmailAddress.of(email), role), hash);
            assertError(call(HttpMethod.POST, "/api/auth/login", "{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}", null), 401, "INVALID_CREDENTIALS");
        }
    }

    @Test void rejectsMissingTamperedExpiredPrematureWrongIssuerAudienceAlgorithmAndMalformedClaims() {
        assertError(call(HttpMethod.GET, "/api/auth/me", null, null), 401, "UNAUTHENTICATED");
        String valid = login("one@example.test").path("accessToken").asText();
        String[] parts = valid.split("\\.");
        parts[2] = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        assertError(call(HttpMethod.GET, "/api/auth/me", null, String.join(".", parts)), 401, "UNAUTHENTICATED");
        UUID id = accounts.findByEmail(EmailAddress.of("one@example.test")).orElseThrow().user().id();
        for (String fault : List.of("expired", "future", "issuer", "audience", "subject", "missing-exp", "missing-role", "role-type", "ttl", "algorithm")) {
            Instant issued = Instant.now().minusSeconds(fault.equals("expired") ? 901 : 1);
            if (fault.equals("future")) issued = Instant.now().plusSeconds(60);
            var claims = JwtClaimsSet.builder().issuer(fault.equals("issuer") ? "other" : "gdrn")
                    .audience(List.of(fault.equals("audience") ? "other" : "gdrn-spa"))
                    .subject(fault.equals("subject") ? "not-a-uuid" : id.toString()).issuedAt(issued).notBefore(issued);
            if (!fault.equals("missing-exp")) claims.expiresAt(issued.plusSeconds(fault.equals("ttl") ? 1800 : 900));
            if (!fault.equals("missing-role")) claims.claim("role", fault.equals("role-type") ? List.of("CITIZEN") : "CITIZEN");
            String token;
            if (fault.equals("algorithm")) {
                var alternate = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(new byte[64]));
                token = alternate.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(), claims.build())).getTokenValue();
            } else token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
            var rejected = call(HttpMethod.GET, "/api/auth/me", null, token);
            assertError(rejected, 401, "UNAUTHENTICATED");
            assertThat(rejected.getHeaders().getFirst("WWW-Authenticate")).isEqualTo("Bearer");
        }
        Instant now = Instant.now().minusSeconds(1);
        var unsupported = JwtClaimsSet.builder().issuer("gdrn").audience(List.of("gdrn-spa"))
                .subject(id.toString()).claim("role", "ADMIN").issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(900)).build();
        assertError(call(HttpMethod.GET, "/api/auth/me", null, encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), unsupported)).getTokenValue()), 403, "FORBIDDEN");
    }

    @Test void rejectsInvalidBodiesQueriesAndContentTypesWithContractErrors() {
        for (String body : List.of("{}", "null", "[]", "{", "{\"email\":null,\"password\":\"x\"}",
                "{\"email\":42,\"password\":\"x\"}", "{\"email\":\"a@b.c\",\"password\":\"x\",\"role\":\"ADMIN\"}",
                "{\"email\":\"a@b.c\",\"password\":\"x\",\"password\":\"y\"}",
                "{\"email\":\"a@b.c\",\"password\":\""+"é".repeat(37)+"\"}")) {
            assertError(call(HttpMethod.POST, "/api/auth/login", body, null), 400, "VALIDATION_ERROR");
        }
        assertError(call(HttpMethod.POST, "/api/auth/login?role=ADMIN", "{\"email\":\"a@b.c\",\"password\":\"x\"}", null), 400, "VALIDATION_ERROR");
        var headers = new HttpHeaders(); headers.setContentType(MediaType.TEXT_PLAIN);
        assertError(http.exchange("/api/auth/login", HttpMethod.POST, new HttpEntity<>("data", headers), JsonNode.class), 415, "UNSUPPORTED_MEDIA_TYPE");
        String token = login("one@example.test").path("accessToken").asText();
        assertError(call(HttpMethod.GET, "/api/auth/me?userId=other", null, token), 400, "VALIDATION_ERROR");
    }

    @Test void demoRerunNeverChangesExistingCredentialsOrRoleAndMissingConfigFailsClearly() {
        var before = jdbc.queryForList("select u.id,u.email,u.role,c.password_hash from identity_users u join identity_credentials c on u.id=c.user_id where u.email in ('one@example.test','two@example.test','ops@example.test') order by email");
        String fresh = UUID.randomUUID().toString();
        environment.getPropertySources().addFirst(new MapPropertySource("demo-test-override", Map.of("DEMO_CITIZEN_ONE_PASSWORD", fresh)));
        try {
            bootstrap.run(new DefaultApplicationArguments());
            assertThat(jdbc.queryForList("select u.id,u.email,u.role,c.password_hash from identity_users u join identity_credentials c on u.id=c.user_id where u.email in ('one@example.test','two@example.test','ops@example.test') order by email")).isEqualTo(before);
            assertThat(before.toString()).doesNotContain(PASSWORD, fresh);
        } finally { environment.getPropertySources().remove("demo-test-override"); }
        environment.getPropertySources().addFirst(new MapPropertySource("demo-test-missing", Map.of("DEMO_CITIZEN_ONE_PASSWORD", "")));
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                    .hasMessage("Demo profile requires DEMO_CITIZEN_ONE_PASSWORD");
        } finally { environment.getPropertySources().remove("demo-test-missing"); }
    }

    private JsonNode login(String email) {
        var response = call(HttpMethod.POST, "/api/auth/login", "{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}", null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody();
    }
    private ResponseEntity<JsonNode> call(HttpMethod method, String path, String body, String token) {
        var headers = new HttpHeaders();
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return http.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
    private void assertError(ResponseEntity<JsonNode> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody().path("status").asInt()).isEqualTo(status);
        assertThat(response.getBody().path("code").asText()).isEqualTo(code);
        assertThat(response.getBody().has("fieldErrors")).isTrue();
        assertThat(response.getBody().toString()).doesNotContain(PASSWORD, SECRET, "stackTrace", "passwordHash");
    }
}
