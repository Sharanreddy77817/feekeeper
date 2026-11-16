# FeeKeeper decisions

## Phase 1

- Phase 1 is backend-only because `INSTRUCTIONS` explicitly requires stopping before later phases; Thymeleaf and mobile clients will follow only after approval.
- UUIDs are generated in the application and stored using PostgreSQL `uuid` columns.
- Email addresses are normalized to lowercase and trimmed; phone numbers are accepted as Indian 10-digit mobile numbers, optionally prefixed by `+91`.
- JWTs are signed with HMAC-SHA256 and carry `userId`, `tenantId`, and `role`; the subject is the user's globally unique email.
- Auth rate limiting is an in-memory, per-IP fixed-window limiter. This is intentionally basic for Phase 1; a shared store is needed before horizontal scaling.
- The tenant Hibernate filter is enabled for authenticated requests and service transactions as defense in depth. Explicit tenant predicates remain mandatory in repositories.
- Registration stores the tenant owner phone in normalized form and creates an OWNER account atomically.
- Added JJWT because Spring Security does not provide JWT creation; added springdoc-openapi and Testcontainers because they are explicitly required by the Phase 1 specification.

## Phase 2

- Phase 2 remains backend-only. No Thymeleaf, JavaScript frontend, or mobile application code was added.
- Member updates treat the supplied join date and plan duration as the complete plan definition and recompute expiry as `joinDate + planDurationDays`.
- Soft-deleted members are excluded by explicit repository predicates. Their phone remains reserved because the per-tenant database uniqueness constraint also protects historical identity.
- `Idempotency-Key` is required for mark-paid, is scoped to a tenant, and is limited to 100 characters. A replay returns the originally stored payment even if the path contains a different member ID, preventing a key from being repurposed.
- Migration V2 keeps the new payment columns nullable so it can safely apply to databases with pre-existing payment history. Every new payment stores both its idempotency key and resulting expiry date.
- Receipt sequences are locked per tenant and calendar year. The member row is locked before the receipt is allocated, so concurrent renewals serialize and calculate expiry from the most recently committed value.
- Dashboard counts use aggregate repository queries. Active is the stored `ACTIVE` status; expiring and overdue totals are derived from expiry dates relative to the injected IST clock. Revenue uses the current IST calendar month.
- Added OpenPDF 2.0.3 because Phase 2 requires server-generated PDF receipts and the JDK/Spring stack has no PDF renderer.
- UPI IDs are optional; when present they must have a handle and provider separated by `@` and are normalized to lowercase.
