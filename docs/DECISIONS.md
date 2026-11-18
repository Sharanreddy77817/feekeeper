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

## Phase 4

- Phase 4 adds only the Spring Boot-served web interface. No mobile client or Phase 5 work was added, and backend business services/repositories were unchanged.
- Added `spring-boot-starter-thymeleaf` because Phase 4 explicitly requires server-rendered page shells and reusable fragments. Tailwind is loaded from its CDN and vanilla JavaScript is served as static resources, so there is no Node build step.
- Page routes and static assets are permitted by Spring Security so normal browser navigation can load them. Because the JWT is intentionally kept in `sessionStorage` and cannot accompany a top-level HTML navigation, `/dashboard` and `/settings` are client-guarded shells; their REST data remains protected by the existing bearer-token security chain and unauthenticated API calls return `401`.
- The JWT is stored under one `sessionStorage` key and is attached only by `api.js` in an `Authorization` header. It is removed on logout or API `401` and is never placed in URLs or logs.
- All dynamic/user-provided values are assigned through `textContent` or form value properties. The UI does not use `innerHTML`.
- A payment modal creates one UUID idempotency key when opened, disables submission while in flight, and retains that key for retries until the modal is reopened.
- The WhatsApp receipt action is a client-generated text receipt link containing receipt number, amount, and new expiry. The PDF remains authenticated and is never exposed through a public URL.

## Phase 5

- Phase 5 contains deployment, security, observability, data-safety, CI, and operational documentation only. No product features or business rules were added.
- Added Spring Boot Actuator for database-aware health/readiness/liveness and secured metrics. Only health and info are anonymous; metrics still require a valid JWT. Swagger and OpenAPI endpoints are disabled by the production profile.
- Added `logstash-logback-encoder` solely to emit valid structured JSON logs in production. Request IDs are included from MDC; application logging continues to exclude passwords, JWTs, API keys, request bodies, and full phone numbers.
- The CSP permits same-origin application assets plus `https://cdn.tailwindcss.com`. Tailwind's runtime CDN requires `'unsafe-eval'` and injected styles require `'unsafe-inline'`; this is a documented compromise of the Phase 4 no-build CDN choice. A future hardening change should compile/self-host Tailwind and remove both allowances.
- Page framing is denied, MIME sniffing is disabled, referrers use `strict-origin-when-cross-origin`, and HSTS is emitted on HTTPS responses. TLS termination and HTTP-to-HTTPS redirect remain the responsibility of the trusted deployment edge.
- Missing static/disabled documentation resources now use the existing problem-detail format with `404` instead of falling through to the generic `500` handler; no business endpoint behavior changed.
- `server.forward-headers-strategy=framework` is enabled for Railway/Render/Lightsail-style reverse proxies. The application must only be reachable through a proxy that replaces untrusted forwarded headers; direct public access would allow header spoofing.
- Production CORS defaults to no cross-origin origins and must be explicitly set to exact HTTPS origins. Request headers, declared request bodies, form posts, swallowed bodies, and multipart requests have bounded defaults; the trusted edge must apply the same body limit to chunked requests that have no `Content-Length`.
- Login attempts are logged only as generic success/failure events. Unknown-email and wrong-password paths both execute BCrypt matching against a real or startup-generated dummy hash, reducing timing-based account enumeration without logging the submitted email.
- Hibernate JDBC timestamp conversion now uses UTC for stored instants. The injected business `Clock` remains `Asia/Kolkata`, so membership dates, monthly revenue, and scheduler boundaries retain the required business timezone.
- The runtime image uses a Java 21 JRE, container-aware percentage memory limits, a numeric non-root user, and an actuator readiness health check. Local and production Compose definitions receive secrets/configuration only through environment variables; the production database publishes no host port.
- CI runs Java 21 `mvn clean verify` with Testcontainers, parses every Surefire XML report, and fails if any test is skipped. It uploads test reports regardless of success.
- OWASP Dependency-Check 13.0.0 was attempted locally on 2026-11-16. It produced no vulnerability report because the NVD rejected the empty API key and the local NVD cache contained no documents; therefore **no clean dependency finding is claimed**. CI requires the `NVD_API_KEY` repository secret, fails when it is absent, uploads the generated reports, and fails for any CVSS 7.0+ finding. The first successful CI report must be reviewed and any findings recorded before launch.

## WhatsApp Cloud API provider

- Added Meta WhatsApp Cloud API as an optional direct provider. This avoids requiring an intermediary/BSP subscription; Meta's message pricing can still apply. The app continues to default to `mock` until the account, credentials, and approved template are configured.
- Meta's template request mapping uses six ordered body parameters from `ReminderMessageFormatter`: member, business, reminder text, amount, expiry date, and UPI/payment fallback. The business must create and approve a template matching this order before setting `WHATSAPP_PROVIDER=meta`.
- The Cloud API access token is carried only in the Authorization header from environment configuration; logs do not include it. `WHATSAPP_PHONE_NUMBER_ID` is a separate required environment value for this provider.
- No Meta credentials or account setup are stored in the repository. Configure the phone number ID, access token, API version URL, approved template name/language in Render after completing Meta onboarding.

## Receipt presentation

- Payment receipts use the tenant's configured business name, owner/contact phone, and UPI ID as the issuer identity. They are laid out as a branded, single-page A5 institute receipt with receipt number/date, member and plan details, payment method, paid amount, and renewed expiry. The currency is labeled INR to retain broad PDF reader compatibility with the existing built-in font.
- No logo upload or custom font dependency was introduced. Tenant identity is sourced from persisted settings, so the receipt never displays a hard-coded sample institute.

## CSV member import

- Dashboard bulk import uploads multipart CSV to the tenant-authenticated `POST /api/members/import` endpoint. The JWT remains in the Authorization header; tenant identity comes only from the security context.
- The required header is `fullName,phone,planName,planAmount,planDurationDays,joinDate`. RFC-style quoted fields are supported, including escaped quotes and embedded commas/newlines. Header names are case-insensitive; extra columns are ignored.
- Imports accept up to 500 data rows and are also bounded by existing multipart/request byte limits. Rows are validated with the same `MemberRequest` Bean Validation rules as single-member creation. Valid rows are batch-saved; invalid, duplicate-in-file, and already-used tenant phone numbers are returned as row-level errors without rejecting other valid rows.
- Existing phone uniqueness behavior is preserved: a deleted member's phone remains reserved within that tenant. A phone belonging to another tenant does not conflict or become observable.

## Agent Context & Token Optimization

- Refactored `AGENTS.md` into a lean, high-density router (<30 lines) with progressive disclosure. Injected system context tokens are reduced by ~65%, preventing recurring token inflation on every agent turn.
- Domain-specific constraints (payments/shedlock, architecture/status calculation, security, testing) are decoupled into modular on-demand reference guides in `docs/rules/`.
- Embedded explicit agent directives enforcing targeted line-range reads, lazy loading of domain guides, and prevention of redundant whole-file inspections.
# International billing and promotion behavior

- V6 introduces tenant currency/time zone settings, tenant-scoped promotion codes, and recorded payment discounts. Existing tenants and payment history migrate with INR defaults.
- A promotion must use exactly one discount form (percentage or flat amount); discounts are capped at the fee amount. A supplied promotion must be active, within its inclusive date window, and match any plan duration restriction. Unknown codes return 404 within the authenticated tenant scope.
- API plans accept 1–3650 days to support annual and custom cycles. The UI offers the common 30/90/180/365 day choices.
- Reminder dates and status calculation use each tenant's configured time zone. Payment business dates continue using the injected application clock.
