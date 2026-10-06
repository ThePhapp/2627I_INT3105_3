# ADR 004: Plan for Internal Domain Events

## Status
Proposed

## Context
Future workflows may require reactions across modules without direct implementation
dependencies. No current business slice establishes event or delivery requirements.

## Decision
When justified by a real use case, evaluate in-process domain/application events.
Keep domain event types framework-independent. Before adopting an implementation,
decide and test dispatch ownership, synchronous/asynchronous execution, transaction
timing, handler failure behavior and delivery guarantees. No event implementation or
external broker is created during bootstrap.

## Alternatives Considered
- Explicit synchronous application contracts: appropriate when a caller needs a result.
- Spring in-process event publication in an outer adapter: candidate, not selected yet.
- External broker: deferred to evidence-based Phase 2 work.

## Consequences
No speculative event API or delivery guarantee constrains future features. In-process
events alone do not provide durable delivery across crashes. Accept or supersede this
ADR when the first actual event use case determines the required semantics.
