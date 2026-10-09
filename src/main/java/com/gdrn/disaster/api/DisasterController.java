package com.gdrn.disaster.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.gdrn.disaster.application.*;
import com.gdrn.disaster.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/disasters")
public class DisasterController {
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final Set<String> LIST_PARAMETERS = Set.of("page", "size", "sort", "status", "type");
    private static final Set<String> UPDATE_FIELDS = Set.of("expectedVersion", "name", "type", "severity",
            "description", "latitude", "longitude", "status");
    private final DisasterService service;

    public DisasterController(DisasterService service) { this.service = service; }

    @GetMapping(produces = "application/json")
    @Operation(operationId = "E03", summary = "List disasters", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PageResponse> list(HttpServletRequest request) {
        rejectBody(request);
        Map<String, String> query = query(request, LIST_PARAMETERS);
        int page = integer(query.getOrDefault("page", "0"), "page");
        int size = integer(query.getOrDefault("size", "20"), "size");
        DisasterSearch.Sort sort = switch (query.getOrDefault("sort", "createdAt,desc")) {
            case "createdAt,asc" -> DisasterSearch.Sort.CREATED_AT_ASC;
            case "createdAt,desc" -> DisasterSearch.Sort.CREATED_AT_DESC;
            default -> throw new InvalidDisasterInput("sort");
        };
        Optional<DisasterStatus> status = optional(query, "status", DisasterInput::status);
        Optional<DisasterType> type = optional(query, "type", DisasterInput::type);
        var result = service.list(new DisasterSearch(page, size, sort, status, type));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PageResponse(
                result.items().stream().map(DisasterResponse::from).toList(), result.page(), result.size(),
                result.totalElements(), result.totalPages()));
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(operationId = "E04", summary = "Get disaster", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DisasterResponse> get(@PathVariable String id, HttpServletRequest request) {
        rejectBody(request);
        if (request.getQueryString() != null) throw new InvalidDisasterInput("query");
        return ok(service.get(id(id)));
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @Operation(operationId = "E05", summary = "Create disaster", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DisasterResponse> create(@RequestBody CreateDisasterRequest body,
                                                    HttpServletRequest request) {
        if (request.getQueryString() != null) throw new InvalidDisasterInput("query");
        if (body == null || body.latitude() == null || body.longitude() == null) {
            throw new InvalidDisasterInput(body == null ? "body" : body.latitude() == null ? "latitude" : "longitude");
        }
        var created = service.create(new DisasterService.Create(body.name(), DisasterInput.type(body.type()),
                DisasterInput.severity(body.severity()), body.description(), body.latitude(), body.longitude()));
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(DisasterResponse.from(created));
    }

    @PatchMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    @Operation(operationId = "E06", summary = "Update disaster", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DisasterResponse> update(@PathVariable String id, @RequestBody JsonNode body,
                                                    HttpServletRequest request) {
        if (request.getQueryString() != null) throw new InvalidDisasterInput("query");
        UpdateDisasterRequest update = UpdateDisasterRequest.from(body);
        Disaster changed = service.update(new DisasterService.Update(id(id), update.expectedVersion(),
                new Disaster.Change(update.name(), update.type(), update.severity(), update.description(),
                        update.latitude(), update.longitude(), update.status())));
        return ok(changed);
    }

    private ResponseEntity<DisasterResponse> ok(Disaster disaster) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(DisasterResponse.from(disaster));
    }

    private static UUID id(String raw) {
        if (raw == null || !UUID_PATTERN.matcher(raw).matches()) throw new InvalidDisasterInput("id");
        return UUID.fromString(raw);
    }

    private static void rejectBody(HttpServletRequest request) {
        if (request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null) {
            throw new InvalidDisasterInput("body");
        }
    }

    private static Map<String, String> query(HttpServletRequest request, Set<String> allowed) {
        Map<String, String> result = new HashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            if (!allowed.contains(name) || values.length != 1 || values[0] == null || values[0].isEmpty()) {
                throw new InvalidDisasterInput(allowed.contains(name) ? name : "query");
            }
            result.put(name, values[0]);
        });
        return result;
    }

    private static int integer(String raw, String field) {
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException error) { throw new InvalidDisasterInput(field); }
    }

    private static <T> Optional<T> optional(Map<String, String> values, String field,
                                             java.util.function.Function<String, T> parser) {
        return values.containsKey(field) ? Optional.of(parser.apply(values.get(field))) : Optional.empty();
    }

    public record CreateDisasterRequest(String name, String type, String severity, String description,
                                        Double latitude, Double longitude) {}
    public record DisasterResponse(UUID id, String name, DisasterType type, Severity severity,
                                   String description, double latitude, double longitude,
                                   DisasterStatus status, long version, Instant createdAt, Instant updatedAt) {
        static DisasterResponse from(Disaster value) {
            return new DisasterResponse(value.id(), value.name(), value.type(), value.severity(),
                    value.description(), value.latitude(), value.longitude(), value.status(), value.version(),
                    value.createdAt(), value.updatedAt());
        }
    }
    public record PageResponse(List<DisasterResponse> items, int page, int size,
                               long totalElements, int totalPages) {}

    private record UpdateDisasterRequest(long expectedVersion, Optional<String> name,
                                         Optional<DisasterType> type, Optional<Severity> severity,
                                         Optional<String> description, Optional<Double> latitude,
                                         Optional<Double> longitude, Optional<DisasterStatus> status) {
        static UpdateDisasterRequest from(JsonNode body) {
            if (body == null || !body.isObject() || body.size() < 2 || !body.has("expectedVersion")) {
                throw new InvalidDisasterInput("body");
            }
            body.fieldNames().forEachRemaining(field -> {
                if (!UPDATE_FIELDS.contains(field)) throw new InvalidDisasterInput("body");
                if (body.get(field).isNull()) throw new InvalidDisasterInput(field);
            });
            JsonNode version = body.get("expectedVersion");
            if (!version.isIntegralNumber() || !version.canConvertToLong()) throw new InvalidDisasterInput("expectedVersion");
            long expected = version.longValue();
            if (expected < 0 || expected > 9_007_199_254_740_991L) throw new InvalidDisasterInput("expectedVersion");
            return new UpdateDisasterRequest(expected, string(body, "name"),
                    string(body, "type").map(DisasterInput::type),
                    string(body, "severity").map(DisasterInput::severity), string(body, "description"),
                    number(body, "latitude"), number(body, "longitude"),
                    string(body, "status").map(DisasterInput::status));
        }
        private static Optional<String> string(JsonNode body, String field) {
            if (!body.has(field)) return Optional.empty();
            if (!body.get(field).isTextual()) throw new InvalidDisasterInput(field);
            return Optional.of(body.get(field).textValue());
        }
        private static Optional<Double> number(JsonNode body, String field) {
            if (!body.has(field)) return Optional.empty();
            if (!body.get(field).isNumber()) throw new InvalidDisasterInput(field);
            double value = body.get(field).doubleValue();
            if (!Double.isFinite(value)) throw new InvalidDisasterInput(field);
            return Optional.of(value);
        }
    }
}
