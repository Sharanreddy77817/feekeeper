# FeeKeeper

FeeKeeper is a multi-tenant fee-management SaaS backend for small Indian gyms and coaching classes.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker (PostgreSQL and integration tests)

## Run locally

1. Copy `.env.example` to `.env` and replace the development values.
2. Start PostgreSQL: `docker compose up -d db`
3. Export the variables from `.env` in your shell.
4. Run: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

OpenAPI is available at `http://localhost:8080/swagger-ui.html`.

## Verify

Run `mvn clean verify`. Integration tests use a disposable PostgreSQL Testcontainer and are skipped automatically only when Docker is unavailable.

## API

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/members`
- `GET /api/members/{id}`
- `PUT /api/members/{id}`
- `DELETE /api/members/{id}` (soft delete)
- `GET /api/members?search=&status=&page=&size=&sort=`
- `GET /api/members/{id}/whatsapp-link`
- `POST /api/payments/mark-paid/{memberId}` (requires `Idempotency-Key`)
- `GET /api/payments?memberId=&fromDate=&toDate=&page=&size=`
- `GET /api/payments/{id}/receipt.pdf`
- `GET /api/dashboard`
- `GET /api/tenant/settings`
- `PUT /api/tenant/settings`

All business endpoints derive their tenant exclusively from the authenticated JWT/SecurityContext. Cross-tenant resources are returned as not found.

The generated OpenAPI 3 document is available at `http://localhost:8080/v3/api-docs`; Swagger UI is at `http://localhost:8080/swagger-ui.html`. Use the `bearerAuth` security scheme with the access token returned by registration or login.

## Phase 2 behavior

- Supported plan durations are 30, 90, and 180 days.
- Member phone numbers are unique per tenant. Soft-deleted members stay excluded from member reads and searches, while their phone remains reserved for audit consistency.
- A payment defaults to the member's plan amount when `amountPaid` is omitted. Modes are `UPI` and `CASH`.
- Reusing an `Idempotency-Key` within a tenant returns the original payment. Receipt numbers use `FK-YYYY-000001` and increment independently per tenant and year.
- Dashboard dates and monthly revenue use the `Asia/Kolkata` business clock.

## Phase 3 reminders

The status and reminder job runs daily at 10:00 Asia/Kolkata. ShedLock stores its distributed lock in PostgreSQL so only one application instance performs the run. It processes members in bounded pages, refreshes statuses through `MemberStatusCalculator`, and sends reminders for memberships expiring in exactly three days or overdue by one through seven days. A successful send records the date; failed sends remain eligible and do not stop the batch.

`WHATSAPP_PROVIDER` selects `mock` (the default), `aisensy`, or `interakt`. For a REST provider, configure:

- `WHATSAPP_BASE_URL` and `WHATSAPP_API_KEY`
- `WHATSAPP_TEMPLATE_NAME` and `WHATSAPP_LANGUAGE_CODE`
- `WHATSAPP_CONNECT_TIMEOUT` and `WHATSAPP_READ_TIMEOUT`
- `WHATSAPP_RETRY_ATTEMPTS` and `WHATSAPP_RETRY_BACKOFF`
- `WHATSAPP_BATCH_SIZE`

Only timeouts and HTTP 5xx responses are retried. The API key, full phone numbers, and provider request bodies are never logged. Provider templates must be approved in the corresponding WhatsApp Business account before using a REST provider.

The tenant-scoped `whatsapp-link` endpoint is a free manual fallback. It returns a URL in the form `https://wa.me/91...?...` with the same centrally formatted English reminder text prefilled.
