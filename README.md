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
