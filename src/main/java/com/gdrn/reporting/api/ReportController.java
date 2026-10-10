package com.gdrn.reporting.api;

import com.gdrn.reporting.application.ReportAccessDenied;
import com.gdrn.reporting.application.ReportActor;
import com.gdrn.reporting.application.ReportService;
import com.gdrn.reporting.application.ReportModerationService;
import com.gdrn.reporting.domain.ReportStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.gdrn.reporting.domain.Coordinates;
import com.gdrn.reporting.domain.InvalidReport;
import com.gdrn.reporting.domain.ReportType;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping(value = "/api/reports", produces = "application/json")
public class ReportController {
    private final ReportService reports;
    private final ReportModerationService moderation;

    public ReportController(ReportService reports, ReportModerationService moderation) {
        this.reports = reports; this.moderation = moderation;
    }

    @PostMapping(consumes = "application/json")
    @Operation(operationId = "E07", summary = "Submit a report")
    public ResponseEntity<ReportResponse> submit(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal,
                                                @RequestBody CreateReportRequest body, HttpServletRequest request) {
        var actor = actor(principal);
        actor.requireCitizen();
        ReportRequestParser.noQuery(request);
        if (body.latitude() == null) throw new InvalidReport("latitude");
        if (body.longitude() == null) throw new InvalidReport("longitude");
        var report = reports.submit(actor, ReportRequestParser.optionalEnum(body.type(), ReportType.class, "type"),
                body.description(), new Coordinates(body.latitude(), body.longitude()));
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(ReportResponse.from(report));
    }

    @GetMapping
    @Operation(operationId = "E08", summary = "List visible reports with optional radius filter")
    public ResponseEntity<PageResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal, HttpServletRequest request) {
        var actor = actor(principal);
        var page = reports.list(actor, ReportRequestParser.filter(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PageResponse(
                page.items().stream().map(ReportResponse::from).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages()));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "E09", summary = "Read a visible report")
    public ResponseEntity<ReportResponse> detail(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal, @PathVariable String id,
                                                HttpServletRequest request) {
        var actor = actor(principal);
        ReportRequestParser.noQuery(request);
        ReportRequestParser.noBody(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ReportResponse.from(
                reports.detail(actor, ReportRequestParser.uuid(id, "id"))));
    }

    @PatchMapping(value = "/{id}/verification", consumes = "application/json")
    @Operation(operationId = "E10", summary = "Verify or reject a pending report")
    public ResponseEntity<ReportResponse> decide(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal,
            @PathVariable String id, @RequestBody JsonNode body, HttpServletRequest request) {
        var actor = actor(principal);
        actor.requireAuthority();
        ReportRequestParser.noQuery(request);
        var decision = ReportRequestParser.decision(body);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ReportResponse.from(
                moderation.decide(actor, ReportRequestParser.uuid(id, "id"), decision)));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "E11", summary = "Withdraw an owned pending report")
    public ResponseEntity<Void> withdraw(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal,
            @PathVariable String id, HttpServletRequest request) {
        var actor = actor(principal);
        actor.requireCitizen();
        ReportRequestParser.noQuery(request);
        ReportRequestParser.noBody(request);
        moderation.withdraw(actor, ReportRequestParser.uuid(id, "id"));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private static ReportActor actor(Jwt principal) {
        // Authentication and claim shape are supplied by P01; no Identity internals are imported.
        if (principal == null) throw new ReportAccessDenied();
        String role = principal.getClaimAsString("role");
        if (!"CITIZEN".equals(role) && !"AUTHORITY".equals(role)) throw new ReportAccessDenied();
        return new ReportActor(UUID.fromString(principal.getSubject()), ReportActor.Role.valueOf(role));
    }

    public record CreateReportRequest(String type, String description, Double latitude, Double longitude) {}
    public record PageResponse(List<ReportResponse> items, int page, int size, long totalElements, long totalPages) {
        public PageResponse { items = List.copyOf(items); }
    }
}
