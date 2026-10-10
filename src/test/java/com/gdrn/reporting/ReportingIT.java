package com.gdrn.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.reset;
import com.gdrn.reporting.application.contract.*;
import com.gdrn.reporting.infrastructure.DisasterQueryAdapter;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CountDownLatch;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.gdrn.reporting.application.ReportFilter;
import com.gdrn.reporting.domain.Coordinates;
import com.gdrn.reporting.domain.Report;
import com.gdrn.reporting.domain.ReportType;
import com.gdrn.reporting.infrastructure.persistence.JdbcReportStore;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Real HTTP/JWT/PostGIS tests using production security and Flyway migrations. */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    @MockitoSpyBean JdbcReportStore store;
    @MockitoSpyBean DisasterQueryAdapter disasterLookup;
    @Autowired ReportingQuery reportingQuery;
    @Autowired JwtEncoder encoder;

    @BeforeEach void resetOnlyThisTestContainerReportingData() {
        reset(store, disasterLookup);
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isEqualTo(5);
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

    @Test void flywayUpgradesV3ToV4AndPreservesExistingIdentityAndDisasterRows() {
        var base = org.flywaydb.core.Flyway.configure().dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
                .schemas("reporting_upgrade_check").target("3").load();
        base.migrate();
        UUID user = UUID.randomUUID();
        UUID disaster = UUID.randomUUID();
        jdbc.update("insert into reporting_upgrade_check.identity_users(id,email,role) values (?, 'upgrade@example.test', 'CITIZEN')", user);
        jdbc.update("""
                insert into reporting_upgrade_check.disasters
                    (id,name,type,severity,description,latitude,longitude,status,version,created_at,updated_at)
                values (?, 'Upgrade fixture', 'FLOOD', 'HIGH', 'Preserved', 0, 0, 'ACTIVE', 0, now(), now())
                """, disaster);
        var upgrade = org.flywaydb.core.Flyway.configure().dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
                .schemas("reporting_upgrade_check").target("4").load();
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(upgrade.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("select count(*) from reporting_upgrade_check.reporting_reports", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select id from reporting_upgrade_check.identity_users", UUID.class)).isEqualTo(user);
        assertThat(jdbc.queryForObject("select id from reporting_upgrade_check.disasters", UUID.class)).isEqualTo(disaster);
    }

    @Test void productionPolicyEnforcesAuthenticationRolesAndDefaultDeny() {
        for (String path : List.of("/api/reports", "/api/reports/" + UUID.randomUUID())) {
            var response = call(HttpMethod.GET, path, null, null);
            assertError(response, 401, "UNAUTHENTICATED");
            assertThat(response.getHeaders().getFirst("WWW-Authenticate")).isEqualTo("Bearer");
        }
        assertError(call(HttpMethod.POST, "/api/reports", null, BODY), 401, "UNAUTHENTICATED");
        assertError(call(HttpMethod.POST, "/api/reports", token(AUTHORITY, "AUTHORITY"), BODY), 403, "FORBIDDEN");
        assertError(call(HttpMethod.GET, "/api/reports", token(ONE, "ADMIN"), null), 403, "FORBIDDEN");
        assertError(call(HttpMethod.GET, "/api/reports", "invalid-token", null), 401, "UNAUTHENTICATED");
        assertError(call(HttpMethod.DELETE, "/api/reports/" + UUID.randomUUID(), token(AUTHORITY, "AUTHORITY"), null), 403, "FORBIDDEN");
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

    @Test void rejectsBadInputUnknownFieldsAndIncompleteSpatialFilters() {
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
                "status=pending", "type=INVALID", "disasterId=1-1-1-1-1", "lat=0", "lat=0&lon=0&radiusMeters=0")) {
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

    @Test void verifiesRejectsAndWithdrawsWithCorrectVisibilityAndResponseShape() {
        UUID disaster = createDisaster();
        UUID verified = createReport(ONE);
        var decided = call(HttpMethod.PATCH, verificationPath(verified), token(AUTHORITY, "AUTHORITY"), verifyBody(disaster));
        assertThat(decided.getStatusCode().value()).isEqualTo(200);
        assertThat(decided.getBody().path("status").asText()).isEqualTo("VERIFIED");
        assertThat(decided.getBody().path("disasterId").asText()).isEqualTo(disaster.toString());
        assertThat(decided.getBody().path("verifiedAt")).isEqualTo(decided.getBody().path("updatedAt"));
        assertThat(decided.getBody().has("rejectionReason")).isFalse();
        assertThat(decided.getBody().has("version")).isFalse();
        assertThat(call(HttpMethod.GET, "/api/reports/" + verified, token(ONE, "CITIZEN"), null).getBody()).isEqualTo(decided.getBody());
        assertError(call(HttpMethod.PATCH, verificationPath(verified), token(AUTHORITY, "AUTHORITY"), verifyBody(UUID.randomUUID())), 409, "INVALID_TRANSITION");
        assertError(call(HttpMethod.DELETE, "/api/reports/" + verified, token(TWO, "CITIZEN"), null), 404, "NOT_FOUND");
        assertError(call(HttpMethod.DELETE, "/api/reports/" + verified, token(ONE, "CITIZEN"), null), 409, "INVALID_TRANSITION");

        UUID rejected = createReport(ONE);
        var rejection = call(HttpMethod.PATCH, verificationPath(rejected), token(AUTHORITY, "AUTHORITY"), "{\"status\":\"REJECTED\",\"rejectionReason\":\"  Insufficient details  \"}");
        assertThat(rejection.getStatusCode().value()).isEqualTo(200);
        assertThat(rejection.getBody().path("rejectionReason").asText()).isEqualTo("Insufficient details");
        assertThat(rejection.getBody().path("rejectedAt")).isEqualTo(rejection.getBody().path("updatedAt"));
        assertThat(rejection.getBody().has("disasterId")).isFalse();
        assertThat(rejection.getBody().has("verifiedAt")).isFalse();
        assertError(call(HttpMethod.DELETE, "/api/reports/" + rejected, token(ONE, "CITIZEN"), null), 409, "INVALID_TRANSITION");

        UUID withdrawn = createReport(ONE);
        assertError(call(HttpMethod.DELETE, "/api/reports/" + withdrawn, token(TWO, "CITIZEN"), null), 404, "NOT_FOUND");
        var response = call(HttpMethod.DELETE, "/api/reports/" + withdrawn, token(ONE, "CITIZEN"), null);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(jdbc.queryForObject("select withdrawn_at is not null from reporting_reports where id=?", Boolean.class, withdrawn)).isTrue();
        assertThat(store.findById(withdrawn)).isEmpty();
        assertThat(reportingQuery.findById(withdrawn)).isEmpty();
        assertError(call(HttpMethod.DELETE, "/api/reports/" + withdrawn, token(ONE, "CITIZEN"), null), 404, "NOT_FOUND");
        assertError(call(HttpMethod.GET, "/api/reports/" + withdrawn, token(AUTHORITY, "AUTHORITY"), null), 404, "NOT_FOUND");
        assertError(call(HttpMethod.PATCH, verificationPath(withdrawn), token(AUTHORITY, "AUTHORITY"), verifyBody(disaster)), 404, "NOT_FOUND");
        var listed = call(HttpMethod.GET, "/api/reports", token(AUTHORITY, "AUTHORITY"), null).getBody();
        assertThat(listed.path("totalElements").asInt()).isEqualTo(2);
        assertThat(listed.toString()).doesNotContain(withdrawn.toString());
    }

    @Test void writeMethodsEnforceRolesAndValidateAllRequestShapes() {
        UUID id = createReport(ONE), disaster = createDisaster();
        String verify = verificationPath(id);
        assertError(call(HttpMethod.PATCH, verify, null, verifyBody(disaster)), 401, "UNAUTHENTICATED");
        assertError(call(HttpMethod.DELETE, "/api/reports/" + id, null, null), 401, "UNAUTHENTICATED");
        for (String role : List.of("CITIZEN", "ADMIN", "RESPONDER")) {
            assertError(call(HttpMethod.PATCH, verify, token(ONE, role), "{}"), 403, "FORBIDDEN");
        }
        for (String role : List.of("AUTHORITY", "ADMIN", "RESPONDER")) {
            assertError(call(HttpMethod.DELETE, "/api/reports/" + id, token(ONE, role), null), 403, "FORBIDDEN");
        }
        for (String body : List.of("{}", "null", "[]", "{", "{\"status\":\"PENDING\",\"rejectionReason\":\"x\"}",
                "{\"status\":\"REJECTED\",\"rejectionReason\":null}", "{\"status\":\"REJECTED\",\"rejectionReason\":\" \"}",
                "{\"status\":\"REJECTED\",\"rejectionReason\":\"" + "x".repeat(501) + "\"}",
                verifyBody(disaster).replace("VERIFIED", "verified"), verifyBody(disaster).replace(disaster.toString(), "1-1-1-1-1"),
                verifyBody(disaster).replace("}", ",\"expectedVersion\":0}"),
                verifyBody(disaster).replace("}", ",\"rejectionReason\":\"x\"}"),
                "{\"status\":\"VERIFIED\",\"disasterId\":null}",
                "{\"status\":\"REJECTED\",\"status\":\"REJECTED\",\"rejectionReason\":\"x\"}")) {
            assertError(call(HttpMethod.PATCH, verify, token(AUTHORITY, "AUTHORITY"), body), 400, "VALIDATION_ERROR");
        }
        assertError(call(HttpMethod.PATCH, verify + "?extra=1", token(AUTHORITY, "AUTHORITY"), verifyBody(disaster)), 400, "VALIDATION_ERROR");
        assertError(call(HttpMethod.DELETE, "/api/reports/" + id + "?extra=1", token(ONE, "CITIZEN"), null), 400, "VALIDATION_ERROR");
        assertError(call(HttpMethod.DELETE, "/api/reports/" + id, token(ONE, "CITIZEN"), "{}"), 400, "VALIDATION_ERROR");
        assertError(call(HttpMethod.DELETE, "/api/reports/not-a-uuid", token(ONE, "CITIZEN"), null), 400, "VALIDATION_ERROR");
        var headers = new HttpHeaders(); headers.setBearerAuth(token(AUTHORITY, "AUTHORITY")); headers.setContentType(MediaType.TEXT_PLAIN);
        assertError(http.exchange(verify, HttpMethod.PATCH, new HttpEntity<>(verifyBody(disaster), headers), JsonNode.class), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(store.findById(id).orElseThrow().status().name()).isEqualTo("PENDING");
    }

    @Test void missingAndResolvedDisasterRejectVerificationAndContractReflectsFreshState() {
        UUID id = createReport(ONE), disaster = createDisaster();
        assertError(call(HttpMethod.PATCH, verificationPath(id), token(AUTHORITY, "AUTHORITY"), verifyBody(UUID.randomUUID())), 404, "NOT_FOUND");
        assertThat(call(HttpMethod.PATCH, verificationPath(id), token(AUTHORITY, "AUTHORITY"), verifyBody(disaster)).getStatusCode().value()).isEqualTo(200);
        assertThat(reportingQuery.findById(id).orElseThrow().disaster().orElseThrow().status()).isEqualTo(LinkedDisasterState.ACTIVE);
        resolveDisaster(disaster);
        assertThat(reportingQuery.findById(id).orElseThrow().disaster().orElseThrow().status()).isEqualTo(LinkedDisasterState.RESOLVED);
        UUID second = createReport(TWO);
        assertError(call(HttpMethod.PATCH, verificationPath(second), token(AUTHORITY, "AUTHORITY"), verifyBody(disaster)), 409, "DISASTER_NOT_ACTIVE");
        var found = reportingQuery.findByIds(Set.of(id, second, UUID.randomUUID()));
        assertThat(found).hasSize(2);
        assertThat(found.get(id).reporterId()).isEqualTo(ONE);
        assertThat(found.get(second).disaster()).isEmpty();
        assertThatThrownBy(found::clear).isInstanceOf(UnsupportedOperationException.class);
        jdbc.update("delete from disasters where id=?", disaster); // Corrupt only isolated fixture: contract must fail closed.
        assertThatThrownBy(() -> reportingQuery.findById(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test void raceTwoVerificationsHasOneSuccessAndOneConflict() throws Exception { race(false, false); }
    @Test void raceVerificationAndWithdrawalHasOneSuccessAndOneConflict() throws Exception { race(false, true); }
    @Test void raceTwoWithdrawalsHasOneSuccessAndOneConflict() throws Exception { race(true, true); }

    private void race(boolean firstWithdraw, boolean secondWithdraw) throws Exception {
        UUID id = createReport(ONE), disaster = createDisaster();
        var barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object pending = invocation.callRealMethod();
            barrier.await(10, TimeUnit.SECONDS);
            return pending;
        }).when(store).findById(id);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> command(id, disaster, firstWithdraw));
            var second = pool.submit(() -> command(id, disaster, secondWithdraw));
            var responses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertThat(responses.stream().filter(r -> r.getStatusCode().is2xxSuccessful()).count()).isEqualTo(1);
            var loser = responses.stream().filter(r -> r.getStatusCode().value() == 409).findFirst().orElseThrow();
            assertError(loser, 409, "INVALID_TRANSITION");
        } finally { reset(store); }
        assertThat(jdbc.queryForObject("select version from reporting_reports where id=?", Long.class, id)).isEqualTo(1);
        var state = jdbc.queryForMap("select status, withdrawn_at from reporting_reports where id=?", id);
        assertThat("VERIFIED".equals(state.get("status")) ^ (state.get("withdrawn_at") != null)).isTrue();
    }

    @Test void resolveAfterFreshBusinessReadDoesNotPreventVerificationCommit() throws Exception {
        UUID id = createReport(ONE), disaster = createDisaster();
        var observed = new CountDownLatch(1); var continueWrite = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object snapshot = invocation.callRealMethod();
            observed.countDown();
            if (!continueWrite.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test barrier timed out");
            return snapshot;
        }).when(disasterLookup).findByIds(Set.of(disaster));
        try (var pool = Executors.newSingleThreadExecutor()) {
            var future = pool.submit(() -> command(id, disaster, false));
            try {
                assertThat(observed.await(10, TimeUnit.SECONDS)).isTrue();
                resolveDisaster(disaster);
            } finally { continueWrite.countDown(); }
            assertThat(future.get(15, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(200);
        } finally { reset(disasterLookup); }
        assertThat(reportingQuery.findById(id).orElseThrow().disaster().orElseThrow().status()).isEqualTo(LinkedDisasterState.RESOLVED);
    }

    @Test void radiusUsesMetersIncludesBoundaryAndCombinesOwnershipStateTypeAndDisaster() {
        UUID center = createReport(ONE), near = createReport(ONE), edge = createReport(ONE), far = createReport(ONE);
        UUID other = createReport(TWO), withdrawn = createReport(ONE);
        for (UUID id : List.of(center, other, withdrawn)) placeAtDistance(id, 0);
        placeAtDistance(near, 999); placeAtDistance(edge, 1000); placeAtDistance(far, 1001);
        assertThat(jdbc.queryForObject("select ST_Distance(location, ST_SetSRID(ST_MakePoint(0,0),4326)::geography) from reporting_reports where id=?", Double.class, edge)).isCloseTo(1000, within(0.00001));
        assertThat(command(withdrawn, null, true).getStatusCode().value()).isEqualTo(204);
        String spatial = "lat=0&lon=0&radiusMeters=1000";
        var own = call(HttpMethod.GET, "/api/reports?" + spatial + "&size=2", token(ONE, "CITIZEN"), null).getBody();
        assertThat(own.path("totalElements").asInt()).isEqualTo(3);
        assertThat(own.path("items").size()).isEqualTo(2);
        assertThat(own.path("totalPages").asInt()).isEqualTo(2);
        var all = call(HttpMethod.GET, "/api/reports?" + spatial, token(AUTHORITY, "AUTHORITY"), null).getBody();
        assertThat(all.path("totalElements").asInt()).isEqualTo(4);
        assertThat(all.toString()).contains(edge.toString()).doesNotContain(far.toString(), withdrawn.toString());
        var page2 = call(HttpMethod.GET, "/api/reports?" + spatial + "&size=2&page=1", token(ONE, "CITIZEN"), null).getBody();
        assertThat(page2.path("items").size()).isEqualTo(1);
        UUID disaster = createDisaster();
        assertThat(command(center, disaster, false).getStatusCode().value()).isEqualTo(200);
        var combined = call(HttpMethod.GET, "/api/reports?" + spatial + "&status=VERIFIED&type=FLOOD&disasterId=" + disaster, token(ONE, "CITIZEN"), null).getBody();
        assertThat(combined.path("totalElements").asInt()).isEqualTo(1);
        assertThat(combined.path("items").get(0).path("id").asText()).isEqualTo(center.toString());
        var otherOwner = call(HttpMethod.GET, "/api/reports?" + spatial + "&disasterId=" + disaster, token(TWO, "CITIZEN"), null).getBody();
        assertThat(otherOwner.path("totalElements").asInt()).isZero();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        transaction.executeWithoutResult(ignored -> {
            jdbc.execute("SET LOCAL enable_seqscan = off");
            var plan = jdbc.queryForList("EXPLAIN SELECT id FROM reporting_reports WHERE withdrawn_at IS NULL AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(0,0),4326)::geography,1000)", String.class);
            assertThat(String.join("\n", plan)).contains("reporting_reports_visible_location_idx");
        });
    }

    @Test void spatialValidationRequiresCompleteFiniteBoundedCoordinatesAndRadius() {
        for (String query : List.of("lat=0", "lon=0", "radiusMeters=1", "lat=0&lon=0", "lat=0&radiusMeters=1",
                "lat=91&lon=0&radiusMeters=1", "lat=0&lon=-181&radiusMeters=1", "lat=NaN&lon=0&radiusMeters=1",
                "lat=Infinity&lon=0&radiusMeters=1", "lat=1e999&lon=0&radiusMeters=1", "lat=&lon=0&radiusMeters=1",
                "lat=0&lon=0&radiusMeters=0", "lat=0&lon=0&radiusMeters=100001", "lat=0&lon=0&radiusMeters=1.5",
                "lat=0&lat=1&lon=0&radiusMeters=1", "lat=0&lon=0&radius=1")) {
            assertError(call(HttpMethod.GET, "/api/reports?" + query, token(ONE, "CITIZEN"), null), 400, "VALIDATION_ERROR");
        }
        assertThat(call(HttpMethod.GET, "/api/reports?lat=-90&lon=180&radiusMeters=100000", token(ONE, "CITIZEN"), null).getStatusCode().value()).isEqualTo(200);
    }

    @Test void flywayV4ToV5PreservesReportsAndEnforcesMetadataConstraints() {
        var config = org.flywaydb.core.Flyway.configure().dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
                .schemas("b2_upgrade").target("4").load();
        config.migrate();
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into b2_upgrade.reporting_reports(id,reporter_id,type,description,location,status,created_at,updated_at)
                values (?,?,'FLOOD','Preserved',ST_SetSRID(ST_MakePoint(0,0),4326)::geography,'PENDING',now(),now())
                """, id, ONE);
        var upgrade = org.flywaydb.core.Flyway.configure().dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
                .schemas("b2_upgrade").target("5").load();
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(upgrade.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("select description from b2_upgrade.reporting_reports where id=?", String.class, id)).isEqualTo("Preserved");
        assertThat(jdbc.queryForObject("select version from b2_upgrade.reporting_reports where id=?", Long.class, id)).isZero();
        assertThatThrownBy(() -> jdbc.update("update b2_upgrade.reporting_reports set status='VERIFIED' where id=?", id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        jdbc.update("update b2_upgrade.reporting_reports set status='VERIFIED', disaster_id=?, verified_at=updated_at,version=1 where id=?", UUID.randomUUID(), id);
        assertThatThrownBy(() -> jdbc.update("update b2_upgrade.reporting_reports set withdrawn_at=updated_at where id=?", id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("select count(*) from b2_upgrade.reporting_reports", Integer.class)).isEqualTo(1);
    }

    @Test void spatialCountAndItemsKeepSameSnapshotWhenWithdrawalCommitsBetweenQueries() throws Exception {
        UUID id = createReport(ONE); placeAtDistance(id, 0);
        var spyJdbc = spy(namedJdbc);
        var consistentStore = new JdbcReportStore(spyJdbc, transactions);
        doAnswer(invocation -> {
            Object count = invocation.callRealMethod();
            try (var worker = Executors.newSingleThreadExecutor()) {
                worker.submit(() -> command(id, null, true)).get(10, TimeUnit.SECONDS);
            }
            return count;
        }).when(spyJdbc).queryForObject(startsWith("select count(*)"), any(SqlParameterSource.class), eq(Long.class));
        var filter = new ReportFilter(0, 20, ReportFilter.Sort.CREATED_DESC, null, null, null,
                new com.gdrn.reporting.application.ReportRadius(new Coordinates(0,0), 1000));
        var page = consistentStore.find(filter, ONE);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items()).hasSize(1);
        assertThat(store.find(filter, ONE).totalElements()).isZero();
    }

    private void placeAtDistance(UUID id, int meters) {
        jdbc.update("update reporting_reports set location=ST_Project(ST_SetSRID(ST_MakePoint(0,0),4326)::geography, ?, pi()/2) where id=?", meters, id);
    }
    private UUID createReport(UUID reporter) {
        var response = call(HttpMethod.POST, "/api/reports", token(reporter, "CITIZEN"), BODY);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return UUID.fromString(response.getBody().path("id").asText());
    }
    private UUID createDisaster() {
        var response = call(HttpMethod.POST, "/api/disasters", token(AUTHORITY, "AUTHORITY"),
                "{\"name\":\"B2 fixture\",\"type\":\"TYPHOON\",\"severity\":\"HIGH\",\"description\":\"Test\",\"latitude\":0,\"longitude\":0}");
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return UUID.fromString(response.getBody().path("id").asText());
    }
    private void resolveDisaster(UUID id) {
        assertThat(call(HttpMethod.PATCH, "/api/disasters/" + id, token(AUTHORITY, "AUTHORITY"), "{\"expectedVersion\":0,\"status\":\"RESOLVED\"}")
                .getStatusCode().value()).isEqualTo(200);
    }
    private String verificationPath(UUID id) { return "/api/reports/" + id + "/verification"; }
    private String verifyBody(UUID disaster) { return "{\"status\":\"VERIFIED\",\"disasterId\":\"" + disaster + "\"}"; }
    private ResponseEntity<JsonNode> command(UUID id, UUID disaster, boolean withdraw) {
        return withdraw ? call(HttpMethod.DELETE, "/api/reports/" + id, token(ONE, "CITIZEN"), null)
                : call(HttpMethod.PATCH, verificationPath(id), token(AUTHORITY, "AUTHORITY"), verifyBody(disaster));
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

}
