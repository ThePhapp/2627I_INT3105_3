# Global Disaster Response Network (GDRN)

**Hệ thống Điều phối và Ứng phó Thảm họa Toàn cầu**

A university Software Architecture project exploring disaster response coordination.
Phase 1 uses a **Modular Monolith + DDD + Hexagonal/Clean Architecture**. This repository
currently contains the bootstrap foundation, not a functioning disaster response product.

**Hướng dẫn cho máy mới:** [Cài đặt và chạy sau khi clone repo (tiếng Việt)](docs/HUONG_DAN_CAI_DAT_VA_CHAY.md).

**Kế hoạch triển khai tiếp theo:** [Pha 1 MVP — 4 người / 3 tuần](docs/KE_HOACH_PHA_1_3_TUAN.md)
và [bộ 16 prompt chia theo người, có thứ tự phụ thuộc](docs/PROMPTS_PHA_1_4_NGUOI.md).
Đây là kế hoạch phát triển, không phải danh sách tính năng đã hoàn thành.

**P00 design baseline (planned):** [HTTP/OpenAPI và 15 API/6 màn hình](docs/api/phase1-contract.md),
[published module contracts và sổ migration](docs/architecture/phase1-module-contracts.md),
[ADR 005: MVP/auth/frontend](docs/adr/005-phase1-mvp-auth-frontend.md),
[tiến độ](docs/phase1-progress.md), [handoff P00](docs/handoffs/P00.md).
MVP ba tuần giới hạn Identity/Disaster/Reporting/Rescue và SPA cơ bản. Resource/Alert,
Geo risk, RescueRequest riêng và các API lớn hơn trong context dài hạn là backlog.
JWT memory-only (reload cần login lại) và frontend là lựa chọn dự kiến, chưa chạy.

## CURRENTLY IMPLEMENTED

- One Java 21 Spring Boot application with Maven Wrapper.
- Plain Java Identity `User`, `EmailAddress`, `Role` and their unit tests; no login yet.
- PostgreSQL/PostGIS infrastructure, Flyway extension migration and JPA configuration.
- Spring Security technical-endpoint policy, Actuator health and empty OpenAPI/Swagger.
- ArchUnit boundary rules and real PostGIS/HTTP integration tests using Testcontainers.
- Docker/Compose, GitHub Actions CI, architecture documentation and four initial ADRs.

## PLANNED

| Module under `com.gdrn` | Responsibility                                                     |
| ----------------------- | ------------------------------------------------------------------ |
| identity                | Authentication, authorization, users, roles                        |
| disaster                | Disaster lifecycle and management                                  |
| reporting               | Citizen incident reporting                                         |
| rescue                  | Requests, teams and missions                                       |
| resource                | Emergency resources and allocations                                |
| alert                   | Emergency alerts                                                   |
| geo                     | Geographic operations and spatial risk functionality               |
| shared                  | Carefully selected technical concerns (configuration exists today) |

No business controllers, services, repositories, entities, tables or REST operations
exist. Login, JWT, role management, domain events and benchmarks are future work.
The original [project context](GDRN_CODEX_PROJECT_CONTEXT.md) describes the eventual
Phase 1 scope; it is not a list of implemented features.

## Stack and repository

Java 21, Spring Boot 3.5.16, Maven 3.9.9, Spring Web/Security/Data JPA/Actuator,
PostgreSQL 16, PostGIS 3.5, Flyway, Springdoc 2.8.17, JUnit 5, Mockito,
Testcontainers and ArchUnit 1.4.2. Boot manages compatible dependency versions
unless explicitly pinned. Hibernate Spatial is deferred until spatial mappings exist.

```text
.github/workflows/ci.yml        Java 21 verification and Docker build
.mvn/wrapper/                  Maven distribution configuration
docs/architecture/             Architecture overview and future package tree
docs/adr/                      Initial architecture decisions
src/main/java/com/gdrn/         Bootstrap and shared technical configuration
src/main/resources/            Application profiles and Flyway migrations
src/test/java/com/gdrn/         Architecture and integration tests
src/test/resources/             Test profile (container supplies connection data)
AGENTS.md                      Instructions for future contributors/agents
Dockerfile                     Multi-stage Java 21 build/runtime
docker-compose.yml             Backend, PostGIS and persistent volume
.env.example                   Safe local configuration placeholders
pom.xml, mvnw, mvnw.cmd         Build and wrappers
```

Reserved business package trees are documented, not populated with fake classes.

## Prerequisites

- JDK 21 for host development; Maven is downloaded by the wrapper on first use.
- Docker Engine/Desktop with Linux containers and Docker Compose v2.
- Network access for the initial Maven and container downloads.
- Free local ports 5432 and 8080 (or adjust `DB_PORT`, `APP_PORT`, and host `DB_URL`).

## Docker setup

PowerShell:

```powershell
Copy-Item .env.example .env
# Edit .env and replace both password placeholders with the same local password.
docker compose config --quiet
docker compose up --build -d --wait
docker compose ps
```

POSIX shells: use `cp .env.example .env`, then the same Docker commands.
Compose reads `.env` automatically. The backend uses `database:5432` on its private
Compose network; published ports bind only to localhost. Database data persists in
the Compose named volume. `docker compose down` stops/removes containers and retains
data. Changing credentials in `.env` does not change an already initialized database's
credentials. Do not remove the data volume unless its contents are disposable.

## Local development (host JVM)

Create/edit `.env` as above, then start only the database and export configuration.
Spring Boot does **not** automatically load `.env`.

```powershell
docker compose up -d --wait database
# Load the simple KEY=value development file; do not execute it as a script.
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

For a trusted, shell-compatible `.env` file in a POSIX shell:

```bash
chmod +x mvnw
docker compose up -d --wait database
set -a
. ./.env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Keep `DB_PASSWORD` equal to `POSTGRES_PASSWORD`, and align host `DB_USERNAME`/`DB_URL`
with the database user, name and published port. Stop the Compose backend before
starting a host backend on the same port. `SERVER_PORT` overrides the host HTTP port;
`APP_PORT` only changes the Compose published port.

## Environment and database

| Variable                     | Purpose                                                      |
| ---------------------------- | ------------------------------------------------------------ |
| `POSTGRES_DB`                | Compose database name; required                              |
| `POSTGRES_USER`              | Compose local database owner; required                       |
| `POSTGRES_PASSWORD`          | Compose local database password; required                    |
| `DB_PORT`                    | Database host port, default 5432                             |
| `APP_PORT`                   | Backend host port in Compose, default 8080                   |
| `DB_URL`                     | Host JDBC URL; local profile defaults to localhost:5432/gdrn |
| `DB_USERNAME`, `DB_PASSWORD` | Host database credentials; required                          |
| `SERVER_PORT`                | Host application listen port, default 8080                   |

`.env.example` contains placeholders only; real `.env` files are ignored. The default
profile requires explicit connection configuration. `application-local.yml` supplies
a host URL default; `application-test.yml` is on the test classpath only and tests
override connection values from an isolated container.

Flyway runs at startup. Its only migration enables PostGIS; there are no business
tables. PostGIS extension objects and Flyway history are technical metadata. Hibernate
uses `ddl-auto: validate`; future schema changes must use migrations following:
**Requirements → Domain Model → Persistence Model → Database Schema → Flyway Migration**.

## Verification

Full verification requires a running Docker daemon; it starts an isolated PostGIS
container, independent of Compose and `.env`:

```powershell
.\mvnw.cmd clean verify
docker compose --env-file .env.example config --quiet
```

POSIX: `./mvnw clean verify`. `./mvnw test` runs architecture/unit tests only and is
useful without Docker, but is **not** the complete verification gate. Failsafe runs
`*IT` integration tests during `verify`, and fails when Docker is unavailable. Tests
cover context startup, Flyway, PostGIS spatial behavior, technical endpoints and
security denials. Future domain/application tests can use JUnit 5 and Mockito.

CI runs clean verify on Java 21 for pushes and pull requests, validates Compose and
builds the runtime image. Image assembly skips test execution only because Docker
build does not provide the Testcontainers daemon; CI verification runs tests first.

If a machine's Maven mirror returns truncated/corrupt artifacts, this optional command
uses Maven Central directly and an isolated ignored cache without altering global settings:

```powershell
.\mvnw.cmd -s .mvn/settings-central.xml "-Dmaven.repo.local=.tools/m2" clean verify
```

The same settings/cache flags can be passed to `spring-boot:run`. Only use this override
where direct Maven Central access is allowed by your development environment.
If an IDE reports unresolved imports, refresh its Maven project configuration before
running. Avoid simultaneous IDE and Maven compilation into `target`: an IDE can overwrite
valid Maven classes with error stubs. Pause automatic IDE compilation during CLI
verification if this occurs; CI and Docker builds use isolated output directories.

## Technical endpoints

- Health: <http://localhost:8080/actuator/health>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

These GET endpoints are public for development; health details are hidden. OpenAPI
has project metadata and no business operations. All other requests are denied.
There is no generated login user, password login or JWT flow.

## Architecture rules and scope

See the [architecture overview](docs/architecture/architecture-overview.md),
[ADRs](docs/adr/) and [agent instructions](AGENTS.md).
Domain is framework-independent; application depends inward; infrastructure implements
ports; controllers handle HTTP and invoke use cases. No module may access another
module's infrastructure or internal implementation. Published cross-module contracts
are specified in P00 for implementation with their owning use cases. ArchUnit rules tolerate currently absent
business classes and automatically check future additions; semantic design needs review.

The bootstrap scope is foundation only. Later Phase 1 work covers domain slices and
a measured baseline. Phase 1 excludes microservices, RabbitMQ, Kafka, Redis,
Kubernetes, API gateways, service discovery, distributed transactions/tracing,
external brokers, separate databases per module and production cloud infrastructure.
Phase 2 may consider selected improvements only after benchmarks identify a concrete
quality-attribute problem, with an ADR and comparable measurements.

Next development stage after P00 review/merge: **P01 Identity & Access and SPA foundation**.
That stage has not been implemented here; P00 does not start P01.
