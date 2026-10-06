# GDRN engineering instructions

Read `GDRN_CODEX_PROJECT_CONTEXT.md` for the long-term project specification and
`docs/architecture/architecture-overview.md` for the current foundation.
The initial bootstrap contains no business features. The context document's JWT,
API, schema, and benchmark examples are future work, not implemented behavior.

## Must

- Preserve the single-application Modular Monolith and module boundaries under `com.gdrn`.
- Follow DDD and Hexagonal/Clean Architecture; dependencies point inward.
- Keep domain code plain Java, independent of Spring, persistence, JSON and HTTP.
- Put business rules in domain and orchestration in application; use ports/adapters.
- Add packages and abstractions only when a real use case needs them.
- Develop requirements → domain model → persistence model → schema → Flyway migration.
- Use explicit reviewed contracts for cross-module calls; document new contracts.
- Test domain rules without Spring, orchestration with mocked ports, and adapters with Testcontainers.
- Update architecture documentation/ADRs when architectural decisions change.
- Run `./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`) before completing work;
  report actual results and environmental blockers. Docker is required for integration tests.
- Keep credentials in environment configuration and real `.env` files out of Git.

## Must not

- Introduce microservices without an approved architecture decision.
- Introduce RabbitMQ, Kafka, Redis or other Phase 2 infrastructure during Phase 1 without evidence and approval.
- Put JPA annotations in domain models or expose persistence entities through REST.
- Put business logic in controllers or persistence/framework details in application.
- Directly access another module's infrastructure or internal implementation classes.
- Bypass architecture tests, silently skip integration tests, or suppress failures to get a green build.
- Add placeholder business classes, speculative base classes, or sample business tables.

`shared` is for small, justified technical concerns, not a dumping ground for domain logic.
In-process events are only a future option; transaction and failure semantics must be
decided and tested with the first event use case. Phase 2 requires measured evidence.
