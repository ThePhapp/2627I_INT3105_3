package com.gdrn.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import com.fasterxml.jackson.databind.JsonNode;
import com.gdrn.reporting.application.ReportFilter;
import com.gdrn.reporting.domain.Coordinates;
import com.gdrn.reporting.domain.Report;
import com.gdrn.reporting.domain.ReportType;
import com.gdrn.reporting.infrastructure.persistence.JdbcReportStore;
import com.gdrn.shared.infrastructure.configuration.SecurityErrorWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Tests the B1 adapters with the PROPOSED route policy and schema, not a production integration claim. */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ReportingIT.ProposedReportingPolicy.class)
class ReportingIT {
    @Container static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"));
    static final UUID ONE = UUID.randomUUID();
    static final UUID TWO = UUID.randomUUID();
    static final UUID AUTHORITY = UUID.randomUUID();
    static final String SECRET = randomKey();
    static final String BODY = """
            {"type":"FLOOD","description":"  Water rising  ","latitude":21.028,"longitude":105.834}
            """;

    private static String randomKey() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
        registry.add("JWT_SECRET_BASE64", () -> SECRET);
    }

    @Autowired TestRestTemplate http;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired NamedParameterJdbcTemplate namedJdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcReportStore store;
    @Autowired JwtEncoder encoder;

    @BeforeEach void installProposalAndResetOnlyThisTestContainerReportingData() {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isEqualTo(2);
        if (jdbc.queryForObject("select to_regclass('reporting_reports')::text", String.class) == null) {
            new ResourceDatabasePopulator(new FileSystemResource("docs/handoffs/b1/reporting-schema.sql")).execute(dataSource);
        }
        jdbc.update("delete from reporting_reports");
    }

    @Test void citizenCreatesPendingAndPostgisRoundTripsWithoutPersistenceLeak() {
        var created = call(HttpMethod.POST, "/api/reports", token(ONE, "CITIZEN"), BODY);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        var body = created.getBody();
        assertThat(body.path("reporterId").asText()).isEqualTo(ONE.toString());
        assertThat(body.path("status").asText()).isEqualTo("PENDING");
        assertThat(body.path("description").asText()).isEqualTo("Water rising");
        assertThat(body.path("latitude").asDouble()).isEqualTo(21.028);
        assertThat(body.path("longitude").asDouble()).isEqualTo(105.834);
        assertThat(body.path("createdAt")).isEqualTo(body.path("updatedAt"));
        assertThat(body.path("createdAt").asText()).endsWith("Z");
        var fields = new java.util.HashSet<String>();
        body.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).isEqualTo(Set.of("id", "reporterId", "type", "description", "latitude", "longitude",
                "status", "createdAt", "updatedAt"));
        UUID id = UUID.fromString(body.path("id").asText());
        var detail = call(HttpMethod.GET, "/api/reports/" + id, token(ONE, "CITIZEN"), null);
        assertThat(detail.getBody()).isEqualTo(body);
        assertThat(detail.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(jdbc.queryForObject("select ST_SRID(location::geometry) from reporting_reports where id = ?", Integer.class, id))
                .isEqualTo(4326);
        assertThat(jdbc.queryForObject("select pg_typeof(location)::text from reporting_reports where id = ?", String.class, id))
                .isEqualTo("geography");
        assertThat(store.findById(id).orElseThrow().coordinates()).isEqualTo(new Coordinates(21.028, 105.834));
    }

    @Test void proposedPolicyEnforcesAuthenticationRolesAndDefaultDeny() {
        for (String path : List.of("/api/reports", "/api/reports/" + UUID.randomUUID())) {
            var response = call(HttpMethod.GET, path, null, null);
            assertError(response, 401, "UNAUTHENTICATED");
            assertThat(response.getHeaders().getFirst("WWW-Authenticate")).isEqualTo("Bearer");
        }
        assertError(call(HttpMethod.POST, "/api/reports", null, BODY), 401, "UNAUTHENTICATED");
        assertError(call(HttpMethod.POST, "/api/reports", token(AUTHORITY, "AUTHORITY"), BODY), 403, "FORBIDDEN");
        assertError(call(HttpMethod.GET, "/api/reports", token(ONE, "ADMIN"), null), 403, "FORBIDDEN");
        assertError(call(HttpMethod.GET, "/api/reports", "invalid-token", null), 401, "UNAUTHENTICATED");
        assertError(call(HttpMethod.DELETE, "/api/reports/" + UUID.randomUUID(), token(ONE, "CITIZEN"), null), 403, "FORBIDDEN");
        assertError(call(HttpMethod.POST, "/api/reports/" + UUID.randomUUID() + "/verification", token(AUTHORITY, "AUTHORITY"), "{}"), 403, "FORBIDDEN");
        assertError(call(HttpMethod.GET, "/api/not-implemented", token(AUTHORITY, "AUTHORITY"), null), 403, "FORBIDDEN");
        assertThat(jdbc.queryForObject("select count(*) from reporting_reports", Integer.class)).isZero();
    }

    @Test void citizenCannotReadOtherCitizenAndAuthorityCanReadBoth() {
        var first = call(HttpMethod.POST, "/api/reports", token(ONE, "CITIZEN"), BODY).getBody();
        var second = call(HttpMethod.POST, "/api/reports", token(TWO, "CITIZEN"), BODY).getBody();
        assertError(call(HttpMethod.GET, "/api/reports/" + first.path("id").asText(), token(TWO, "CITIZEN"), null), 404, "NOT_FOUND");
        assertError(call(HttpMethod.GET, "/api/reports/" + second.path("id").asText(), token(ONE, "CITIZEN"), null), 404, "NOT_FOUND");
        assertError(call(HttpMethod.GET, "/api/reports/" + UUID.randomUUID(), token(ONE, "CITIZEN"), null), 404, "NOT_FOUND");
        var own = call(HttpMethod.GET, "/api/reports", token(ONE, "CITIZEN"), null).getBody();
        assertThat(own.path("totalElements").asInt()).isEqualTo(1);
        assertThat(own.path("items").get(0)).isEqualTo(first);
        var all = call(HttpMethod.GET, "/api/reports", token(AUTHORITY, "AUTHORITY"), null).getBody();
        assertThat(all.path("totalElements").asInt()).isEqualTo(2);
        assertThat(call(HttpMethod.GET, "/api/reports/" + first.path("id").asText(), token(AUTHORITY, "AUTHORITY"), null).getBody()).isEqualTo(first);
    }

    @Test void paginationAndFiltersAreAppliedBeforeCountWithStableIdTieBreaks() {
        Instant time = Instant.parse("2026-10-09T01:00:00Z");
        UUID low = UUID.fromString("00000000-0000-4000-8000-000000000001");
        UUID high = UUID.fromString("00000000-0000-4000-8000-000000000002");
        store.insert(Report.submit(high, ONE, ReportType.FLOOD, "High", new Coordinates(0, 0), time));
        store.insert(Report.submit(low, ONE, ReportType.FLOOD, "Low", new Coordinates(0, 0), time));
        store.insert(Report.submit(UUID.randomUUID(), TWO, ReportType.TYPHOON, "Other", new Coordinates(0, 0), time.plusSeconds(1)));
        for (String direction : List.of("asc", "desc")) {
            var page = call(HttpMethod.GET, "/api/reports?size=1&page=0&sort=createdAt," + direction + "&status=PENDING&type=FLOOD",
                    token(AUTHORITY, "AUTHORITY"), null).getBody();
            assertThat(page.path("items").get(0).path("id").asText()).isEqualTo(low.toString());
            assertThat(page.path("totalElements").asInt()).isEqualTo(2);
            assertThat(page.path("totalPages").asInt()).isEqualTo(2);
            assertThat(page.path("page").asInt()).isZero();
            assertThat(page.path("size").asInt()).isEqualTo(1);
        }
        var second = call(HttpMethod.GET, "/api/reports?size=1&page=1", token(ONE, "CITIZEN"), null).getBody();
        assertThat(second.path("items").get(0).path("id").asText()).isEqualTo(high.toString());
        var beyond = call(HttpMethod.GET, "/api/reports?size=1&page=3", token(ONE, "CITIZEN"), null).getBody();
        assertThat(beyond.path("items")).isEmpty();
        assertThat(beyond.path("totalElements").asInt()).isEqualTo(2);
        for (String filter : List.of("status=VERIFIED", "status=REJECTED", "type=TSUNAMI", "disasterId=" + UUID.randomUUID())) {
            var empty = call(HttpMethod.GET, "/api/reports?" + filter, token(AUTHORITY, "AUTHORITY"), null);
            assertThat(empty.getStatusCode().value()).isEqualTo(200);
            assertThat(empty.getBody().path("items")).isEmpty();
            assertThat(empty.getBody().path("totalElements").asInt()).isZero();
            assertThat(empty.getBody().path("totalPages").asInt()).isZero();
        }
    }

    @Test void rejectsBadInputUnknownFieldsAndUnimplementedSpatialFilters() {
        String token = token(ONE, "CITIZEN");
        for (String body : List.of("{}", "null", "[]", "{", BODY.replace("21.028", "91"), BODY.replace("105.834", "181"),
                BODY.replace("21.028", "null"), BODY.replace("21.028", "\"21.028\""), BODY.replace("FLOOD", "flood"),
                BODY.replace("Water rising", " "), BODY.replace("Water rising", "x".repeat(2001)),
                BODY.replace("\"type\":", "\"reporterId\":\"" + TWO + "\",\"type\":"),
                BODY.replace("\"type\":", "\"role\":\"AUTHORITY\",\"type\":"),
                BODY.replace("\"type\":", "\"status\":\"VERIFIED\",\"type\":"),
                BODY.replace("\"type\":", "\"type\":\"FLOOD\",\"type\":"))) {
            assertError(call(HttpMethod.POST, "/api/reports", token, body), 400, "VALIDATION_ERROR");
        }
        for (String query : List.of("ownerId=" + TWO, "reporterId=" + TWO, "page=-1", "page=1000001", "size=0", "size=101",
                "size=1&size=2", "page=", "size=1.5", "page=9999999999999", "sort=id,asc", "sort=createdAt,asc&sort=createdAt,desc",
                "status=pending", "type=INVALID", "disasterId=1-1-1-1-1", "lat=0", "lat=0&lon=0&radiusMeters=1000")) {
            assertError(call(HttpMethod.GET, "/api/reports?" + query, token, null), 400, "VALIDATION_ERROR");
        }
        assertError(call(HttpMethod.GET, "/api/reports/1-1-1-1-1", token, null), 400, "VALIDATION_ERROR");
        assertError(call(HttpMethod.GET, "/api/reports/AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", token, null), 400, "VALIDATION_ERROR");
        assertError(call(HttpMethod.GET, "/api/reports", token, "{}"), 400, "VALIDATION_ERROR");
        assertError(call(HttpMethod.POST, "/api/reports?ownerId=" + TWO, token, BODY), 400, "VALIDATION_ERROR");
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.TEXT_PLAIN);
        assertError(http.exchange("/api/reports", HttpMethod.POST, new HttpEntity<>(BODY, headers), JsonNode.class), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(jdbc.queryForObject("select count(*) from reporting_reports", Integer.class)).isZero();
    }

    @Test void countAndItemsShareSnapshotAcrossConcurrentCommittedInsert() throws Exception {
        var spyJdbc = spy(namedJdbc);
        var consistentStore = new JdbcReportStore(spyJdbc, transactions);
        var report = Report.submit(UUID.randomUUID(), ONE, ReportType.FLOOD, "Concurrent", new Coordinates(0, 0), Instant.now());
        doAnswer(invocation -> {
            Object count = invocation.callRealMethod();
            try (var worker = Executors.newSingleThreadExecutor()) {
                worker.submit(() -> store.insert(report)).get(10, TimeUnit.SECONDS);
            }
            return count;
        }).when(spyJdbc).queryForObject(startsWith("select count(*)"), any(SqlParameterSource.class), eq(Long.class));
        var filter = new ReportFilter(0, 20, ReportFilter.Sort.CREATED_DESC, null, null, null);
        var page = consistentStore.find(filter, ONE);
        assertThat(page.totalElements()).isZero();
        assertThat(page.items()).isEmpty();
        assertThat(store.find(filter, ONE).totalElements()).isEqualTo(1);
    }

    @Test void schemaRejectsInvalidStateAndNoCrossModuleForeignKeyExists() {
        var report = Report.submit(UUID.randomUUID(), ONE, ReportType.FLOOD, "Valid", new Coordinates(-90, 180), Instant.now());
        store.insert(report);
        assertThat(store.findById(report.id()).orElseThrow().coordinates()).isEqualTo(report.coordinates());
        assertThatThrownBy(() -> jdbc.update("update reporting_reports set status = 'VERIFIED' where id = ?", report.id()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("select count(*) from pg_constraint where conrelid = 'reporting_reports'::regclass and contype = 'f'", Integer.class))
                .isZero();
    }

    String token(UUID id, String role) {
        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject(id.toString()).issuer("gdrn").audience(List.of("gdrn-spa"))
                        .issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(900)).claim("role", role).build())).getTokenValue();
    }

    ResponseEntity<JsonNode> call(HttpMethod method, String path, String token, String body) {
        var headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    void assertError(ResponseEntity<JsonNode> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        var body = response.getBody();
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("code").asText()).isEqualTo(code);
        assertThat(body.path("path").asText()).doesNotContain("?");
        assertThat(body.path("fieldErrors").isArray()).isTrue();
        assertThat(body.has("timestamp")).isTrue();
        assertThat(body.has("message")).isTrue();
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(body.toString()).doesNotContain("reporting_reports", "stackTrace", "accessToken");
    }

    /** Test-only wiring of the requested handoff policy; production SecurityConfiguration remains untouched. */
    @TestConfiguration(proxyBeanMethods = false)
    static class ProposedReportingPolicy {
        @Bean @Order(0)
        SecurityFilterChain proposedReportingChain(HttpSecurity http, SecurityErrorWriter errors) throws Exception {
            var converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                String role = jwt.getClaimAsString("role");
                return "CITIZEN".equals(role) || "AUTHORITY".equals(role)
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role)) : List.of();
            });
            return http.securityMatcher("/api/reports", "/api/reports/*")
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                    .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                    .logout(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .requestCache(c -> c.disable())
                    .authorizeHttpRequests(a -> a
                            .requestMatchers(HttpMethod.POST, "/api/reports").hasRole("CITIZEN")
                            .requestMatchers(HttpMethod.GET, "/api/reports", "/api/reports/*").hasAnyRole("CITIZEN", "AUTHORITY")
                            .anyRequest().denyAll())
                    .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                            .authenticationEntryPoint((r, s, e) -> errors.write(r, s, 401))
                            .accessDeniedHandler((r, s, e) -> errors.write(r, s, 403)))
                    .exceptionHandling(e -> e.authenticationEntryPoint((r, s, x) -> errors.write(r, s, 401))
                            .accessDeniedHandler((r, s, x) -> errors.write(r, s, 403)))
                    .build();
        }
    }
}
