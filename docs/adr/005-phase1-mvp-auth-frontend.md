# ADR 005: Scope MVP, planned JWT authentication and frontend

## Status

Proposed for team review — P00 design baseline, 2026-10-08. Not implemented.
Next unused number after ADR 004; ADR 001–004 remain unchanged. Acceptance by the
team must be recorded; this document does not claim approval or runtime support.

## Context

Four contributors have 15 working days for a real citizen-to-rescue flow. The current
repository has bootstrap infrastructure and plain Java User/EmailAddress/Role with
tests, but no business API, credential store or frontend. The long-term specification
contains larger role/module/API/event proposals that cannot all fit this MVP.

## Decision for implementation

Use the [three-week plan](../KE_HOACH_PHA_1_3_TUAN.md) and exactly
[E01–E15 / S01–S06](../api/phase1-contract.md). Keep one Spring Boot modular monolith,
Java 21 and one PostgreSQL/PostGIS database. Identity, Disaster, Reporting and Rescue
are the active business slices. Geo radius filtering belongs to Reporting's adapter.
Resource, Alert, separate RescueRequest, priority/risk algorithms, uploads, interactive
maps, registration/reset/refresh, CRUD team and more lifecycle states remain backlog.
No event bus in MVP; ADR 004 remains a future proposal. Deferred product scope does
not automatically require Phase 2 infrastructure; measured evidence still gates it.

Dependencies are Rescue → Reporting published query → Disaster published query.
[Module contracts](../architecture/phase1-module-contracts.md) specify DTO signatures,
batch/missing behavior, reference integrity and migration ordering. Resolve is an
independent Disaster command. ACTIVE eligibility is checked at query time; it does
not promise cross-module atomicity at assignment commit. DB constraints protect
mission uniqueness and atomic writes protect state transitions within each module.

### Planned authentication

- JWT Bearer only, stateless; Spring Security compatible JWT libraries. No hand-written
  cryptography/parser; exact library/version compatibility confirmed by P01.
- Single active role per user: CITIZEN or AUTHORITY. Preserve existing Java Role's
  RESPONDER/ADMIN and tests; no MVP seed, login token issuance, routes or permissions
  for these roles. Login of an unsupported role gets generic INVALID_CREDENTIALS;
  otherwise-valid token carrying unsupported role gets 403 FORBIDDEN.
- Sign HS256 only with environment `JWT_SECRET_BASE64`, decoded key at least 32 random
  bytes. Issuer `gdrn`, audience `gdrn-spa`, TTL exactly 900 seconds. Missing/invalid
  key must fail configuration, never fall back to a built-in secret. Key not in bundle.
- Required claims: `sub` canonical user UUID, `role`, `iss`, `aud`, `iat`, `nbf`, `exp`.
  Issued `nbf=iat`, `exp=iat+900`. Validate fixed algorithm, signature, issuer, audience,
  subject/claim types, lifetime and not-before; zero clock skew for deterministic MVP
  expiry, hosts need correct clocks. At `now >= exp` reject 401. No header-supplied
  key URL, `none` algorithm or algorithm fallback. User id/role come from verified claims.
- No refresh token, revocation list or logout endpoint. Browser keeps JWT and user
  data **in memory only**. Reload/new tab requires login again. Logout clears token,
  user and per-user caches; issued JWT remains valid to expiration. A stolen token
  remains usable up to 15 minutes. No token in localStorage/sessionStorage/URL/logs.
- Password hashing uses Spring PasswordEncoder/BCrypt, cost 10 baseline. API password
  max 72 UTF-8 bytes avoids truncation; no trim. Two citizen + one authority supplied
  through controlled `demo` profile using environment credentials; never reset an
  existing account's password/role on rerun, never put credentials in migrations.
- API security remains central, method/route allowlist, unknown API default deny.
  Object ownership remains in module application; no reliance on UI role guards.
  No cookie auth, Basic or form login alongside Bearer. Planned CSRF policy: disable
  for stateless `/api/**` because browsers do not attach memory-held Bearer tokens
  automatically; P01 must implement/test and record exact matcher configuration.
  Keep bootstrap policy until P01. If cookie auth is introduced this decision must
  be revisited. Dev proxy/same-origin production avoids broad CORS; no wildcard credentials.

### Planned frontend

React + TypeScript + Vite SPA in `frontend/`, React Router, small fetch wrapper and
shared CSS/form/loading/error components. No SSR/Redux/design system framework.
P01 verifies compatible package/Node versions, pins Node and commits lockfile, creates
typecheck/test/build scripts. Feature owners supply route exports; Person 1 integrates
router/client/shared dependencies. `npm ci` thereafter; no independent lockfile edits.

S01 public; S02/S03 citizen; S04/S05/S06 authority. E02 after login validates current
session; 401 clears memory and returns to S01. 403 displays denial, 409 refetches and
asks user to review without auto retry. Report progress uses E09 + E14, not a reverse
Reporting dependency. Production demo serves SPA and `/api` proxy from one origin;
this is a static server configuration, not a new API gateway architecture.

## Alternatives and consequences

Cookie sessions could avoid manual Bearer handling but need a different CSRF/session
contract. Refresh/long-lived browser storage would improve reload UX but add token
lifecycle/storage risk. The selected memory-only short-lived JWT is intentionally
simple and forces re-login on reload/expiry; logout cannot invalidate an issued token.

A larger frontend or long-term modules would reduce time available for ownership,
concurrency tests and real E2E. Six small screens cover every planned operation.
No current implementation is rewritten by this ADR. P01 and feature owners implement
later, tests validate actual behavior; A1/X1 audit the integrated contract and A3
closes remaining evidence. Team review items are in [P00 handoff](../handoffs/P00.md).
