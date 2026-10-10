package com.gdrn.reporting.api;

import com.gdrn.reporting.application.ReportFilter;
import com.gdrn.reporting.application.ReportRadius;
import com.gdrn.reporting.application.ReportModerationService;
import com.gdrn.reporting.domain.Coordinates;
import com.fasterxml.jackson.databind.JsonNode;
import com.gdrn.reporting.domain.InvalidReport;
import com.gdrn.reporting.domain.ReportStatus;
import com.gdrn.reporting.domain.ReportType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

final class ReportRequestParser {
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Set<String> FILTERS = Set.of("page", "size", "sort", "status", "type", "disasterId", "lat", "lon", "radiusMeters");

    private ReportRequestParser() {}

    static void noQuery(HttpServletRequest request) {
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) throw new InvalidReport("query");
    }

    static void noBody(HttpServletRequest request) {
        if (request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null) {
            throw new InvalidReport("body");
        }
    }

    static UUID uuid(String value, String field) {
        if (value == null || !UUID_PATTERN.matcher(value).matches()) throw new InvalidReport(field);
        return UUID.fromString(value);
    }

    static ReportFilter filter(HttpServletRequest request) {
        noBody(request);
        Map<String, String[]> query = request.getParameterMap();
        for (var entry : query.entrySet()) {
            if (!FILTERS.contains(entry.getKey()) || entry.getValue().length != 1) throw new InvalidReport("query");
        }
        String sort = request.getParameter("sort");
        var order = sort == null || sort.equals("createdAt,desc") ? ReportFilter.Sort.CREATED_DESC
                : sort.equals("createdAt,asc") ? ReportFilter.Sort.CREATED_ASC : null;
        String disaster = request.getParameter("disasterId");
        return new ReportFilter(integer(request.getParameter("page"), 0, "page"),
                integer(request.getParameter("size"), 20, "size"), order,
                optionalEnum(request.getParameter("status"), ReportStatus.class, "status"),
                optionalEnum(request.getParameter("type"), ReportType.class, "type"),
                disaster == null ? null : uuid(disaster, "disasterId"), radius(request));
    }

    private static ReportRadius radius(HttpServletRequest request) {
        String lat = request.getParameter("lat"), lon = request.getParameter("lon"), meters = request.getParameter("radiusMeters");
        if (lat == null && lon == null && meters == null) return null;
        if (lat == null || lon == null || meters == null) throw new InvalidReport("radiusMeters");
        return new ReportRadius(new Coordinates(decimal(lat, "lat"), decimal(lon, "lon")), integer(meters, 0, "radiusMeters"));
    }

    private static double decimal(String value, String field) {
        if (!value.matches("-?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) throw new InvalidReport(field);
        try {
            double result = Double.parseDouble(value);
            if (!Double.isFinite(result)) throw new InvalidReport(field);
            return result;
        } catch (NumberFormatException error) { throw new InvalidReport(field); }
    }

    static ReportModerationService.Decision decision(JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 2 || !body.path("status").isTextual()) throw new InvalidReport("body");
        var status = optionalEnum(body.get("status").textValue(), ReportStatus.class, "status");
        if (status == ReportStatus.VERIFIED && body.path("disasterId").isTextual()) {
            return new ReportModerationService.Decision(status, uuid(body.get("disasterId").textValue(), "disasterId"), null);
        }
        if (status == ReportStatus.REJECTED && body.path("rejectionReason").isTextual()) {
            return new ReportModerationService.Decision(status, null, body.get("rejectionReason").textValue());
        }
        throw new InvalidReport("body");
    }

    static <T extends Enum<T>> T optionalEnum(String value, Class<T> type, String field) {
        if (value == null) return null;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ex) { throw new InvalidReport(field); }
    }

    private static int integer(String value, int fallback, String field) {
        if (value == null) return fallback;
        if (!value.matches("[0-9]+")) throw new InvalidReport(field);
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ex) { throw new InvalidReport(field); }
    }
}
