# ADR 001: Use a Modular Monolith

## Status
Accepted

## Context
A four-person university team needs a manageable Phase 1 baseline and clear domain
boundaries before measuring bottlenecks. No distribution requirement is established.

## Decision
Use one Spring Boot application with modules under `com.gdrn` and one database
infrastructure. Reserve identity, disaster, reporting, rescue, resource, alert, geo
and shared. Enforce dependencies with ArchUnit and code review.

## Alternatives Considered
- Layer-only monolith: simple, but obscures business ownership boundaries.
- Microservices: adds deployment and consistency concerns before evidence justifies them.

## Consequences
Deployment and transactions remain local. Modules share a process, database and
release lifecycle; boundaries need discipline. Independent scaling is not provided.
Any later extraction requires an ADR and benchmark evidence; no speedup is asserted.
