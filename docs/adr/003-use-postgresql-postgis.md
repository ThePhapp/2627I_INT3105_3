# ADR 003: Use PostgreSQL with PostGIS

## Status
Accepted

## Context
Planned coordination use cases involve relational consistency and geographic queries.
The bootstrap needs reproducible database infrastructure but no business schema.

## Decision
Use one PostgreSQL 16 database with PostGIS 3.5, Docker Compose for local development,
Flyway for migrations and Testcontainers for integration tests. The first migration
only enables PostGIS. Hibernate validates schema. Spatial ORM dependencies are deferred
until mapped attributes need them. Connection credentials come from the environment.

## Alternatives Considered
- Plain PostgreSQL: lacks PostGIS spatial operations needed by planned requirements.
- H2 for integration tests: does not verify PostgreSQL/PostGIS behavior.
- One database per module: unnecessary operational and consistency cost in this phase.

## Consequences
Developers need Docker for full verification. Extension installation requires suitable
database privileges. Shared database access must respect module ownership in code.
Future schemas follow domain analysis, and query/index decisions require real workloads.
