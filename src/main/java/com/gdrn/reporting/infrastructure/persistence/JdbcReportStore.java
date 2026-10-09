package com.gdrn.reporting.infrastructure.persistence;

import com.gdrn.reporting.application.ReportFilter;
import com.gdrn.reporting.application.ReportPage;
import com.gdrn.reporting.application.port.ReportStore;
import com.gdrn.reporting.domain.Coordinates;
import com.gdrn.reporting.domain.Report;
import com.gdrn.reporting.domain.ReportType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Explicit data mapper: PostGIS stays here; no JDBC or geometry library escapes the adapter. */
@Repository
public class JdbcReportStore implements ReportStore {
    private static final String SELECT = """
            select id, reporter_id, type, description, status, created_at, updated_at,
                   ST_Y(location::geometry) as latitude, ST_X(location::geometry) as longitude
            from reporting_reports
            """;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate reads;

    public JdbcReportStore(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.reads = new TransactionTemplate(transactions);
        reads.setReadOnly(true);
        reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override public Report insert(Report report) {
        jdbc.update("""
                insert into reporting_reports
                    (id, reporter_id, type, description, location, status, created_at, updated_at)
                values (:id, :reporter, :type, :description,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :status, :created, :updated)
                """, new MapSqlParameterSource()
                .addValue("id", report.id()).addValue("reporter", report.reporterId())
                .addValue("type", report.type().name()).addValue("description", report.description())
                .addValue("latitude", report.coordinates().latitude()).addValue("longitude", report.coordinates().longitude())
                .addValue("status", report.status().name())
                .addValue("created", OffsetDateTime.ofInstant(report.createdAt(), ZoneOffset.UTC))
                .addValue("updated", OffsetDateTime.ofInstant(report.updatedAt(), ZoneOffset.UTC)));
        return report;
    }

    @Override public Optional<Report> findById(UUID id) {
        return jdbc.query(SELECT + " where id = :id", new MapSqlParameterSource("id", id),
                JdbcReportStore::map).stream().findFirst();
    }

    @Override public ReportPage find(ReportFilter filter, UUID restrictedReporterId) {
        var parameters = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" where true");
        if (restrictedReporterId != null) {
            where.append(" and reporter_id = :reporter");
            parameters.addValue("reporter", restrictedReporterId);
        }
        if (filter.status() != null) {
            where.append(" and status = :status");
            parameters.addValue("status", filter.status().name());
        }
        if (filter.type() != null) {
            where.append(" and type = :type");
            parameters.addValue("type", filter.type().name());
        }
        if (filter.disasterId() != null) {
            where.append(" and disaster_id = :disaster");
            parameters.addValue("disaster", filter.disasterId());
        }
        String order = filter.sort() == ReportFilter.Sort.CREATED_ASC ? "asc" : "desc";
        parameters.addValue("limit", filter.size()).addValue("offset", (long) filter.page() * filter.size());
        // Both statements share a REPEATABLE READ snapshot, even if an insert commits between them.
        return reads.execute(ignored -> {
            long count = jdbc.queryForObject("select count(*) from reporting_reports" + where, parameters, Long.class);
            var items = jdbc.query(SELECT + where + " order by created_at " + order
                    + ", id asc limit :limit offset :offset", parameters, JdbcReportStore::map);
            return new ReportPage(items, filter.page(), filter.size(), count);
        });
    }

    private static Report map(ResultSet row, int index) throws SQLException {
        if (!"PENDING".equals(row.getString("status"))) {
            throw new IllegalStateException("Unsupported persisted report state; B2 schema/model must be integrated together.");
        }
        return Report.reconstitutePending(row.getObject("id", UUID.class), row.getObject("reporter_id", UUID.class),
                ReportType.valueOf(row.getString("type")), row.getString("description"),
                new Coordinates(row.getDouble("latitude"), row.getDouble("longitude")),
                row.getObject("created_at", OffsetDateTime.class).toInstant(),
                row.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
