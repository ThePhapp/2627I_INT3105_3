-- Allocated during B1/C1 integration after V3 Disaster; no business seed.
CREATE TABLE reporting_reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL,
    type VARCHAR(16) NOT NULL CHECK (type IN ('EARTHQUAKE', 'FLOOD', 'TYPHOON', 'WILDFIRE', 'TSUNAMI')),
    description TEXT NOT NULL CHECK (char_length(description) BETWEEN 1 AND 2000 AND btrim(description) <> ''),
    location geography(Point, 4326) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status = 'PENDING'),
    -- Used by the public disasterId filter; all B1 reports are unlinked PENDING reports.
    disaster_id UUID CHECK (disaster_id IS NULL),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at >= created_at)
);

CREATE INDEX reporting_reports_created_idx ON reporting_reports (created_at DESC, id ASC);
CREATE INDEX reporting_reports_reporter_created_idx ON reporting_reports (reporter_id, created_at DESC, id ASC);
-- No cross-module FK/join. B2 will extend state constraints/verification/withdrawal columns
-- and add the spatial index with its measured ST_DWithin query, in a newly allocated migration.
