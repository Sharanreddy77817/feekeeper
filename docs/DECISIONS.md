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
