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

## Phase 1 API

- `POST /api/auth/register`
- `POST /api/auth/login`

All later business endpoints will derive their tenant exclusively from the authenticated JWT/SecurityContext.
