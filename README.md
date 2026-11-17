# FeeKeeper

FeeKeeper is a multi-tenant fee-management SaaS for small Indian gyms and coaching classes, with a Spring Boot API and responsive Thymeleaf interface.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop or Docker Engine (PostgreSQL, integration tests, and container builds)
- PostgreSQL 16 for non-container local development

## Run locally

1. Copy `.env.example` to `.env` and replace `DB_PASSWORD` and `JWT_SECRET`.
2. Start the complete stack: `docker compose up --build`.
3. Open `http://localhost:8080/register`.

To run Spring Boot from Maven instead, start only PostgreSQL with `docker compose up -d db`, export the `.env` values into the shell, keep `DB_URL=jdbc:postgresql://localhost:5432/feekeeper`, and run `mvn spring-boot:run -Dspring-boot.run.profiles=dev`.

Open `http://localhost:8080/register` to create the first business, or `/login` for an existing account. The dashboard supports member search and filtering, add/edit/delete, one-click renewal, PDF receipt download/printing, and WhatsApp reminders. Business details and logout are under `/settings`.

The interface uses Tailwind's CDN build, so the browser needs internet access for styling. There is no Node installation or frontend build step. OpenAPI remains available at `http://localhost:8080/swagger-ui.html`.

## Verify

Run `mvn clean verify`. Integration tests use disposable PostgreSQL Testcontainers. Locally they are reported as skipped when Docker is unavailable; CI fails if any test is skipped.

Build the production image with `docker build -t feekeeper:local .`. Its health check calls `/actuator/health/readiness`.

## Environment variables

| Variable | Required in production | Purpose / example |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Yes | Set to `prod`. |
| `PORT` | Host-dependent | HTTP port; defaults to `8080`. Railway/Render may supply it. |
| `DB_URL` | Yes | JDBC URL such as `jdbc:postgresql://private-db:5432/feekeeper`. |
| `DB_USER` / `DB_PASSWORD` | Yes | PostgreSQL credentials with access only to the FeeKeeper database. |
| `JWT_SECRET` | Yes | Random secret of at least 32 bytes; rotating it signs out existing sessions. |
| `JWT_EXPIRY_MINUTES` | No | Access-token lifetime; default `60`. |
| `CORS_ALLOWED_ORIGINS` | Yes | Exact comma-separated HTTPS origins; production default is empty. |
| `AUTH_RATE_LIMIT_ATTEMPTS` | No | Auth attempts per fixed window; production default `5`. |
| `AUTH_RATE_LIMIT_WINDOW_MINUTES` | No | Auth rate-limit window; default `1`. |
| `WHATSAPP_PROVIDER` | No | `mock` (default), `meta`, `aisensy`, or `interakt`; keep `mock` until setup and templates are approved. |
| `WHATSAPP_BASE_URL` / `WHATSAPP_API_KEY` | For REST provider | Provider endpoint and secret. For Meta use `https://graph.facebook.com/v27.0` and a Meta access token. |
| `WHATSAPP_PHONE_NUMBER_ID` | For Meta | Phone number ID from the WhatsApp Cloud API; store it in the host environment, not source control. |
| `WHATSAPP_TEMPLATE_NAME` / `WHATSAPP_LANGUAGE_CODE` | For REST provider | Approved template settings. |
| `WHATSAPP_CONNECT_TIMEOUT` / `WHATSAPP_READ_TIMEOUT` | No | Defaults `3s` / `10s`. |
| `WHATSAPP_RETRY_ATTEMPTS` / `WHATSAPP_RETRY_BACKOFF` | No | Defaults `3` / `500ms`. |
| `WHATSAPP_BATCH_SIZE` | No | Scheduler page size; default `100`. |
| `APP_VERSION` | No | Version shown by `/actuator/info`. |
| `MAX_HTTP_HEADER_SIZE` | No | Default `16KB`. |
| `MAX_FORM_POST_SIZE` / `MAX_SWALLOW_SIZE` | No | Tomcat request limits; each defaults to `1MB`. |
| `MAX_FILE_SIZE` / `MAX_REQUEST_SIZE` | No | Multipart limits; each defaults to `1MB`. |
| `MAX_REQUEST_BODY_BYTES` | No | Rejects requests with a declared body larger than 1 MiB; the trusted edge should enforce the same limit for chunked requests. |

`COMPOSE_DB_URL`, `PROD_DB_URL`, `POSTGRES_DB`, `DB_PORT`, `APP_PORT`, and `APP_CONTAINER_PORT` are Compose conveniences documented in `.env.example`.

## Production deployment

FeeKeeper can run from the `Dockerfile` on Railway, Render, AWS Lightsail, or another Docker host:

1. Provision private managed PostgreSQL and enable automated backups/PITR. Do not expose port 5432 publicly.
2. Deploy the repository as a Docker service and set `SPRING_PROFILES_ACTIVE=prod` plus the required variables above. Convert provider URLs to the JDBC form required by `DB_URL` if necessary.
3. Configure the platform health path as `/actuator/health/readiness`; liveness is `/actuator/health/liveness`.
4. Attach the custom domain and HTTPS. The trusted edge proxy must replace untrusted forwarded headers.
5. Set `CORS_ALLOWED_ORIGINS` to the final HTTPS origin. Confirm Swagger is disabled and `/actuator/metrics` returns `401` without a bearer token.
6. On first startup, Flyway applies migrations automatically. Register the first customer only after readiness reports `UP`.

For a single Docker host, copy `docker-compose.prod.example.yml`, supply environment variables through the host secret manager or protected `.env`, and run `docker compose -f docker-compose.prod.example.yml up -d --build`. The example database is on an internal Docker network and has no published port.

## First-customer onboarding

1. Complete [the launch checklist](docs/LAUNCH_CHECKLIST.md) and a tested backup restore.
2. Register the customer's real business and owner details over HTTPS; verify phone and UPI ID before sending reminders.
3. Add a small set of representative plans/members, verify status and expiry calculations, then record a low-value test payment and inspect its receipt.
4. Keep WhatsApp in mock mode until approved templates and provider credentials are ready. Test the manual `wa.me` fallback with the customer.
5. Explain corrections, soft deletion, receipt download, logout, support, privacy notice, retention, and deletion-request handling.

## Bulk member import

From the dashboard, choose **CSV template** to download a sample, fill it, then choose **Import CSV**. Required columns are fullName, phone, planName, planAmount, planDurationDays, and joinDate. Phone numbers must be valid Indian mobile numbers; plan duration must be 30, 90, or 180 days; dates use YYYY-MM-DD. Quoted CSV fields can contain commas. Each file accepts up to 500 data rows and follows the configured upload size limit. Valid rows import even if other rows fail. The result reports imported/failed counts and row-specific errors. A phone already in this business or repeated in the same file is reported as a row error; other businesses' phone records are not visible to the import.

## API

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/members`
- `POST /api/members/import` (multipart field `file`, CSV; up to 500 rows)
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

`WHATSAPP_PROVIDER` selects `mock` (the default), `meta`, `aisensy`, or `interakt`. Meta Cloud API is supported directly without a BSP subscription. Meta message charges may still apply. Before enabling it, create a Meta business/WhatsApp account, register a sending number, create and get approval for a utility reminder template, then set these Render environment values:

- `WHATSAPP_PROVIDER=meta`
- `WHATSAPP_BASE_URL=https://graph.facebook.com/v27.0`
- `WHATSAPP_API_KEY` to the Meta access token (keep it secret)
- `WHATSAPP_PHONE_NUMBER_ID` to the registered phone number ID
- `WHATSAPP_TEMPLATE_NAME` and `WHATSAPP_LANGUAGE_CODE` to the approved template

The approved template body must use six text placeholders in this order: member name (`{{1}}`), business name (`{{2}}`), reminder variant text (`{{3}}`), amount (`{{4}}`), expiry date (`{{5}}`), and UPI ID or fallback instructions (`{{6}}`). For example: `Hello {{1}}, a reminder from {{2}}: {{3}} Amount due: ₹{{4}}. Expiry date: {{5}}. UPI ID: {{6}}. Thank you.` Only use this after Meta approves it and members have opted in to reminders. Keep mock selected until setup is complete.

For AiSensy or Interakt, configure:

- `WHATSAPP_BASE_URL` and `WHATSAPP_API_KEY`
- `WHATSAPP_TEMPLATE_NAME` and `WHATSAPP_LANGUAGE_CODE`
- `WHATSAPP_CONNECT_TIMEOUT` and `WHATSAPP_READ_TIMEOUT`
- `WHATSAPP_RETRY_ATTEMPTS` and `WHATSAPP_RETRY_BACKOFF`
- `WHATSAPP_BATCH_SIZE`

Only timeouts and HTTP 5xx responses are retried. The API key, full phone numbers, and provider request bodies are never logged. Provider templates must be approved in the corresponding WhatsApp Business account before using a REST provider.

The tenant-scoped `whatsapp-link` endpoint is a free manual fallback. It returns a URL in the form `https://wa.me/91...?...` with the same centrally formatted English reminder text prefilled.

## Phase 4 web interface

- `/login` and `/register` handle authentication and store the access token in browser `sessionStorage`.
- `/dashboard` and `/settings` are page shells guarded by JavaScript; every business API remains server-protected and redirects to login after a `401`.
- Shared API behavior lives in `static/js/api.js`, including bearer headers, problem-detail parsing, session expiry, field errors, and authenticated blob downloads.
- Dynamic member and error content is rendered with `textContent`; user-controlled HTML is never injected.
- See [docs/MANUAL_TESTS.md](docs/MANUAL_TESTS.md) for the full mobile, payment, receipt, settings, logout, and cross-tenant checklist.
