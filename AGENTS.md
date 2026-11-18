# AGENTS.md: FeeKeeper (Gym & Coaching Fee Management SaaS)

## Non-Negotiable Core Invariants
- **Stack**: Java 21, Spring Boot 3.3+, Maven, PostgreSQL, Spring Security (stateless JWT), Flyway (`ddl-auto: validate`), Lombok.
- **Multi-Tenancy**: All business tables have non-null `tenant_id`. `tenantId` is sourced strictly from `TenantContext` (never request body/params/headers). Every repository query filters by `tenantId`. Cross-tenant access returns 404 (not 403). New endpoints require isolation tests.
- **Security**: Passwords BCrypt. `JWT_SECRET` from env (min 32 bytes; fail prod start if invalid). Never log credentials, tokens, or full phones. Zero secrets in repo.
- **Domain Core**: Injected `Clock` (Asia/Kolkata). Currency is `BigDecimal(12,2)`. Controllers use DTO records only (never entities). Global error advice returns RFC problem-detail (no stack traces).

## Token-Efficient Agent Directives
- **Lazy Rule Loading**: Do NOT read domain rule files up front. Only inspect a sub-rule file when actively modifying that subsystem.
- **Targeted File Inspection**: Use offset reading (`StartLine`/`EndLine`) for targeted method inspection. Never read entire 500+ line files when a snippet is needed.
- **No Redundant Re-reading**: Retain file contexts in-memory within the session; avoid re-reading unmodified files.
- **Minimal Verbosity**: Keep explanations, command outputs, and code edits focused and minimal.

## On-Demand Rule Index
Read the corresponding reference *only* when working on that domain:
- **Payments & Scheduler**: `docs/rules/payments.md` (pessimistic lock, idempotency key, ShedLock, receipt sequence, WhatsApp).
- **Architecture & DTOs**: `docs/rules/architecture.md` (package structure, Flyway rules, `MemberStatusCalculator`, Thymeleaf UI).
- **Security & Auth**: `docs/rules/security.md` (JWT handling, auth rate-limiting, error responses).
- **Testing Standards**: `docs/rules/testing.md` (Testcontainers, required isolation tests).

## Commands & Definition of Done
- Build & test: `mvn clean verify` | Run tests: `mvn test` | Local dev: `docker-compose up -d db && mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- Always verify `mvn clean verify` passes before finishing.
- No TODOs in security, payment, or scheduler code. Record any assumptions in `docs/DECISIONS.md`.
