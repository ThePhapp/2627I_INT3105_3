# ADR 008: Reporting atomic decisions and published query

## Status

Implemented on the B2 branch from main `ca3751d`; pending review/merge.
Extends ADR 007 without changing the P00 wire contract.

## Decision

Report owns a one-way PENDING → VERIFIED/REJECTED lifecycle. Withdrawal keeps the
PENDING status and records withdrawn_at; all visible single/batch/list queries exclude
withdrawn rows. Version is internal and is never an E10/E11 request or response field.

Each command first authorizes the actor and reads the visible report, then checks
PENDING. Verification checks a fresh Disaster snapshot through Reporting's
DisasterLookup port and its outer DisasterQueryAdapter. The adapter imports only
the published Disaster contract and explicitly maps enums into Reporting types.

The only write is an atomic SQL UPDATE conditional on id, observed version, PENDING
and withdrawn_at IS NULL. One affected row commits the decision and increments
version; zero rows maps to INVALID_TRANSITION (409). No read lock is held across the
Disaster lookup. Missing/withdrawn on the initial read maps to 404; other ownership
also maps to 404 before state checks. This preserves P00 retry and race semantics.
Resolving a disaster after the eligibility read does not block verification's commit;
there is no cross-module transaction, lock, foreign key or ACTIVE-at-commit promise.

V5 extends V4 with metadata/version/lifecycle constraints and a partial GiST index
on geography for non-withdrawn reports. E08 uses ST_DWithin with WGS84 geography and
integer meters, before pagination/count. Count and items keep the existing read-only
REPEATABLE READ transaction. The index matches the query expression and visibility
predicate; its availability is tested with EXPLAIN, not claimed as a benchmark gain.

ReportingQuery performs one batch report query, gathers distinct VERIFIED disaster
IDs and performs at most one Disaster batch lookup through the port. It returns
immutable P00 snapshots, omits missing/withdrawn reports, and fails if a VERIFIED
reference is missing. It does not return persistence objects, tombstones or deletion
flags. Consumers own authorization; the trusted internal provider does not filter by actor.

## Evidence and consequences

Plain Java tests cover lifecycle and orchestration through mocked ports. PostGIS
integration tests exercise production HTTP/security, V4→V5 upgrade, inclusive radius,
ownership, query snapshots and barrier-controlled competing commands. A separate
latch-controlled test covers resolve after eligibility read. See [B2 handoff](../handoffs/B2.md).

No event bus, service split or additional infrastructure is required. Future code
that introduces an outer transaction must re-evaluate isolation and query freshness;
current commands perform a single atomic persistence write after eligibility checks.
