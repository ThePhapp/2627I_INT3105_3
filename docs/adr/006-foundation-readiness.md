# ADR 006: Database-aware readiness for the foundation

- Status: Accepted
- Date: 2026-10-09
- Scope: Technical runtime foundation; no business API or infrastructure addition.

## Context

Compose must report the backend healthy only when it can accept requests and use
its database. A live JVM alone cannot demonstrate this. Conversely, a temporary
database outage should not make the application's liveness fail.

## Decision

Use the existing Spring Boot Actuator availability probes in every environment.
`/actuator/health/liveness` includes only `livenessState`.
`/actuator/health/readiness` includes `readinessState` and `db`.
The Docker image healthcheck uses readiness, so `docker compose up --wait` waits
for an application ready to use PostgreSQL. Flyway and JPA validation still execute
during startup. No custom business controller or health indicator is needed.

Expose public GET access only to the aggregate health and these two exact probe
paths, alongside existing Swagger/OpenAPI routes. Hide health details; deny direct
component paths and write methods. There is no automatic restart policy tied to
database failure, and no orchestration platform is introduced.

## Consequences and verification

Database failure yields readiness HTTP 503 while liveness remains HTTP 200.
Explicit refusal of traffic yields readiness 503 even if the DB is reachable.
Recovery returns readiness to 200. Tests pause/unpause only their isolated PostGIS
Testcontainer to exercise a real database outage; short connection/socket timeouts
are test-only. These tests do not stop the developer's database or delete data.

The DB health indicator checks connectivity, not all future business invariants or
PostGIS query correctness; migration/spatial integration tests cover the foundation.
The public probes expose status without connection details or credentials.

Reference: [Spring Boot 3.5 health groups and availability probes](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html).
