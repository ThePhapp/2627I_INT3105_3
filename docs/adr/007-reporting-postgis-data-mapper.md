# ADR 007: Reporting PostGIS data mapper

## Status

Implemented during B1/C1 integration. V4 follows C1 V3; centralized security exposes E07–E09.

## Context

E07 creates a PENDING report from an authenticated citizen. E08/E09 read it under
ownership rules. B2 will add verification, withdrawal and radius filtering. Reporting
needs a WGS84 point now, but B1 does not own root dependencies or centralized security.

## Decision

Use a plain Java Report/Coordinates model and a ReportStore application port.
JdbcReportStore is an explicit data mapper using the existing Spring JDBC dependency;
no new Hibernate Spatial/root dependency is needed. SQL, ResultSet and PostGIS stay
inside infrastructure. HTTP DTOs and domain models contain no persistence types.
Identity's existing JPA mapping remains unchanged.

Persist `geography(Point,4326)` using longitude as X, latitude as Y; see the
[PostGIS 3.5 constructor contract](https://www.postgis.net/docs/manual-3.5/ST_MakePoint.html).
Use scalar UUID references with no cross-module FK, join or repository access.
The B1 schema permits PENDING only and unlinked disaster_id; that column supports the
contract's disasterId predicate, which correctly matches no B1 rows.

List count and items execute under one read-only REPEATABLE READ transaction in the
adapter. The application port promises a consistent request snapshot. PostgreSQL's
[isolation semantics](https://www.postgresql.org/docs/16/transaction-iso.html) support
this without locking writers; a real concurrent insert test verifies the behavior.
Sort always adds id ASC. There is no snapshot guarantee across multiple HTTP requests.
Create is one atomic SQL insert. Timestamp generation is UTC, truncated to microseconds
before insert so creation and subsequent reads agree with PostgreSQL precision.

## Consequences

Flyway V4 creates the Reporting schema after V3 Disaster. Tests run production
migrations and centralized JWT security; upgrade V3→V4 preserves existing rows.
Runtime OpenAPI publishes the implemented Reporting slice, excluding B2 spatial filters.

B2 must extend the domain reconstitution/DTO mapper for VERIFIED/REJECTED, allocate
a new migration for state/verification/withdrawal data and replace the PENDING-only
constraints. It must filter withdrawn rows in every normal query, implement atomic
transition writes and add the spatial index with the actual radius query. No B2
command or published ReportingQuery implementation is introduced by B1.

JDBC means explicit mapping/SQL maintenance and explicit transaction configuration.
Using Hibernate Spatial remains an option if a later use case justifies it; no
second persistence representation is introduced merely to satisfy an ORM convention.
