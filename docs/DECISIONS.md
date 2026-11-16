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

## Phase 3

- Phase 3 contains scheduler and WhatsApp notification backend work only. No frontend, mobile, or Phase 4 code was added.
- Added ShedLock Spring and JDBC-provider dependencies because the daily job must have a PostgreSQL-backed distributed lock across application instances. Migration V3 creates only ShedLock's lock table.
- `SystemMemberRepository` is the sole intentional cross-tenant data path. It is a JDBC-only repository in the notification package, is not referenced by any controller, and exists only for the scheduled system job. API repositories remain explicitly tenant-scoped.
- Status refresh uses UUID keyset pages and calls `MemberStatusCalculator` for every non-deleted member, preserving the calculator as the single source of status rules without loading the full member table.
- Reminder scanning also uses bounded UUID keyset pages. Eligibility is exactly expiry `today + 3`, or expiry from `today - 7` through `today - 1`; deleted members and records already sent today are skipped.
- Notification delivery is at-least-once across process crashes: `lastReminderSentDate` is written only after provider success. ShedLock and the same-day marker prevent normal duplicate runs, but a crash after provider acceptance and before the marker update may require provider-side idempotency in a future provider integration.
- The REST notification implementation uses the JDK HTTP client through Spring `RestClient`, so no additional HTTP-client dependency is needed. Connect/read timeouts are configurable; retries with backoff apply only to timeouts/connection failures and HTTP 5xx responses.
- AiSensy and Interakt payload shapes are isolated behind provider mappers. Template name, language, and message parameters originate from the central English formatter; a future Hindi formatter can consume the same `ReminderMessage` contract.
- The manual wa.me endpoint uses digits-only international addressing (`91` plus a 10-digit Indian number), as required by wa.me, even though stored/displayed normalization uses `+91`.
- For members who are not overdue, the manual wa.me fallback uses the upcoming-expiry template. The scheduled job uses that template only at exactly three days before expiry.
