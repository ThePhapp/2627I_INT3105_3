-- Identity owns both tables. No accounts or credentials are seeded by migrations.
CREATE TABLE identity_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CITIZEN', 'AUTHORITY', 'RESPONDER', 'ADMIN'))
);
CREATE TABLE identity_credentials (
    user_id UUID PRIMARY KEY REFERENCES identity_users(id),
    password_hash VARCHAR(100) NOT NULL
);
