# ADR 002: Use Hexagonal/Clean Architecture

## Status
Accepted

## Context
Future business rules must be testable without HTTP or persistence frameworks and
must survive infrastructure changes without leaking framework types into domain code.

## Decision
Dependencies point inward. API invokes application use cases; domain is plain Java.
Infrastructure implements application/domain ports and maps persistence models.
Introduce abstractions only with concrete requirements. ArchUnit checks structural rules.

## Alternatives Considered
- Controllers calling Spring Data directly: unsuitable for the required boundary discipline.
- Framework-annotated domain objects: less mapping, but couples domain to persistence.
- Speculative generic base classes: more abstraction without an established use case.

## Consequences
Future slices require explicit interfaces and mappings where boundaries need them.
Domain tests can run without Spring. Structural tests cannot prove semantic placement
of business logic; review remains necessary. Bootstrap creates no fake inner-layer types.
