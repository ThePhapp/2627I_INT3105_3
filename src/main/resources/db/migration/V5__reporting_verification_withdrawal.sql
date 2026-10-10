-- B2 extends applied V4 without rewriting it. References remain scalar IDs.
ALTER TABLE reporting_reports
    DROP CONSTRAINT reporting_reports_status_check,
    DROP CONSTRAINT reporting_reports_disaster_id_check,
    ADD COLUMN verified_at TIMESTAMPTZ,
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN rejected_at TIMESTAMPTZ,
    ADD COLUMN withdrawn_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    ADD CONSTRAINT reporting_reports_lifecycle_check CHECK (
        (status = 'PENDING' AND disaster_id IS NULL AND verified_at IS NULL
            AND rejection_reason IS NULL AND rejected_at IS NULL)
        OR (status = 'VERIFIED' AND disaster_id IS NOT NULL AND verified_at IS NOT NULL
            AND rejection_reason IS NULL AND rejected_at IS NULL AND withdrawn_at IS NULL)
        OR (status = 'REJECTED' AND disaster_id IS NULL AND verified_at IS NULL
            AND rejection_reason IS NOT NULL AND rejected_at IS NOT NULL AND withdrawn_at IS NULL)
    ),
    ADD CONSTRAINT reporting_reports_reason_check CHECK (
        rejection_reason IS NULL OR (char_length(rejection_reason) BETWEEN 1 AND 500 AND btrim(rejection_reason) <> '')
    ),
    ADD CONSTRAINT reporting_reports_event_times_check CHECK (
        (verified_at IS NULL OR (verified_at >= created_at AND verified_at = updated_at))
        AND (rejected_at IS NULL OR (rejected_at >= created_at AND rejected_at = updated_at))
        AND (withdrawn_at IS NULL OR (withdrawn_at >= created_at AND withdrawn_at = updated_at))
    );

CREATE INDEX reporting_reports_visible_location_idx ON reporting_reports USING GIST (location)
    WHERE withdrawn_at IS NULL;
