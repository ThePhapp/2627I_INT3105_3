CREATE TABLE disasters (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL CHECK (btrim(name) <> ''),
    type VARCHAR(20) NOT NULL CHECK (type IN ('EARTHQUAKE', 'FLOOD', 'TYPHOON', 'WILDFIRE', 'TSUNAMI')),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MODERATE', 'HIGH', 'CRITICAL')),
    description VARCHAR(2000) NOT NULL CHECK (btrim(description) <> ''),
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'RESOLVED')),
    version BIGINT NOT NULL CHECK (version BETWEEN 0 AND 9007199254740991),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL CHECK (updated_at >= created_at)
);

CREATE INDEX disasters_created_at_id_idx ON disasters (created_at DESC, id ASC);
CREATE INDEX disasters_status_created_at_id_idx ON disasters (status, created_at DESC, id ASC);
CREATE INDEX disasters_type_created_at_id_idx ON disasters (type, created_at DESC, id ASC);
