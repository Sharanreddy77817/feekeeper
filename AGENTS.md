# AGENTS.md: FeeKeeper (Gym & Coaching Fee Management SaaS)

## Project overview

Multi-tenant SaaS for small Indian gyms and coaching classes to track member fees, record payments, and send WhatsApp reminders. Java 21, Spring Boot 3.3+, Maven, PostgreSQL, Spring Security (stateless JWT), Flyway, Lombok.

## Commands

- Build and test: `mvn clean verify`
- Run locally: `docker-compose up -d db && mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- Run tests only: `mvn test`
- Always run `mvn clean verify` before finishing a task. Do not leave the build broken.

## Architecture

- Package by feature: auth, tenant, member, payment, dashboard, notification, common.
- Layers: controller -> service -> repository. Controllers use DTOs (Java records) only, never entities.
- Schema changes only through new Flyway migrations. Never edit an applied migration. `ddl-auto` must be `validate`.
- Money is BigDecimal, numeric(12,2). Never double or float.
- Use an injected `Clock` for all time logic. Business timezone is Asia/Kolkata.
- Status rules live in one place, `MemberStatusCalculator`:
  OVERDUE if expiry < today; EXPIRING_SOON if today <= expiry <= today + 5 days; otherwise ACTIVE.

## Multi-tenancy rules (non-negotiable)

- Every business table has a non-null `tenant_id`.
- tenantId comes only from the authenticated SecurityContext via `TenantContext`. Never from request body, params, or headers.
- Every repository query on business data must filter by tenantId.
- Cross-tenant access returns 404, not 403.
- Any new endpoint needs a cross-tenant isolation test.

## Security rules

- Passwords: BCrypt. JWT secret from env `JWT_SECRET` (min 32 bytes); the app must fail to start in prod without it.
- Never log passwords, tokens, or full phone numbers.
- No secrets in the repo. Update `.env.example` when adding config.
- Validate all input with Bean Validation. Escape user content in the frontend.
- Rate-limit auth endpoints.

## Payments and scheduler rules

- mark-paid runs in one transaction with a pessimistic lock on the member row and supports an Idempotency-Key.
- New expiry = max(currentExpiry, today) + planDurationDays.
- Receipt numbers are unique per tenant, generated via a locked sequence row.
- The daily 10:00 IST job uses ShedLock. Duplicate reminders are prevented with `lastReminderSentDate`, updated only after a successful send. One failing member must not stop the batch.
- WhatsApp provider is selected by `WHATSAPP_PROVIDER` (mock | aisensy | interakt). Default is mock.

## Testing requirements

- JUnit 5 + Mockito for unit tests; Testcontainers (PostgreSQL) for integration tests.
- Required tests: tenant isolation, mark-paid logic, scheduler selection and no-duplicate sends, status calculator, auth.
- New code needs tests. Do not delete or weaken existing tests to make the build pass.

## Code style

- Constructor injection, no field injection. Small focused methods. No unused code.
- Consistent error responses via the global `@RestControllerAdvice` (problem-detail JSON, no stack traces).
- Frontend: Thymeleaf + Tailwind CDN + vanilla JS, mobile-first.

## Working agreement

- Work in small steps and commit after each logical unit with a clear message.
- If a requirement is ambiguous, choose the simplest safe option and record it in `docs/DECISIONS.md`.
- Do not add new dependencies without a one-line justification in `docs/DECISIONS.md`.
- Do not change tenant isolation, auth, or payment logic outside the task scope.
- At the end of each task, summarize what changed, how to verify it, and any known gaps.

## Definition of done

- `mvn clean verify` passes.
- No TODOs in security, payment, or scheduler code.
- README and `.env.example` are up to date.
