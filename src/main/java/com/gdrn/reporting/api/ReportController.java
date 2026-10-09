package com.gdrn.reporting.api;

import com.gdrn.reporting.application.ReportAccessDenied;
import com.gdrn.reporting.application.ReportActor;
import com.gdrn.reporting.application.ReportService;
import com.gdrn.reporting.domain.Coordinates;
import com.gdrn.reporting.domain.InvalidReport;
import com.gdrn.reporting.domain.ReportType;
import io.swagger.v3.oas.annotations.Hidden;
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

// Do not advertise inaccessible endpoints until the integration owner installs schema + route policy.
// See docs/handoffs/B1.md; remove Hidden in the same integration patch.
@Hidden
@RestController
@RequestMapping(value = "/api/reports", produces = "application/json")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) { this.reports = reports; }

    @PostMapping(consumes = "application/json")
    @Operation(operationId = "E07", summary = "Submit a report")
    public ResponseEntity<ReportResponse> submit(@AuthenticationPrincipal Jwt principal,
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
    @Operation(operationId = "E08", summary = "List visible reports (spatial filters pending B2)")
    public ResponseEntity<PageResponse> list(@AuthenticationPrincipal Jwt principal, HttpServletRequest request) {
        var actor = actor(principal);
        var page = reports.list(actor, ReportRequestParser.filter(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PageResponse(
                page.items().stream().map(ReportResponse::from).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages()));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "E09", summary = "Read a visible report")
    public ResponseEntity<ReportResponse> detail(@AuthenticationPrincipal Jwt principal, @PathVariable String id,
                                                HttpServletRequest request) {
        var actor = actor(principal);
        ReportRequestParser.noQuery(request);
        ReportRequestParser.noBody(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ReportResponse.from(
                reports.detail(actor, ReportRequestParser.uuid(id, "id"))));
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
