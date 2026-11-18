# Security & Authentication Rules

## Authentication & Credentials
- **Password Hashing**: BCrypt with secure salt generation.
- **JWT Configuration**: Secret sourced strictly from `JWT_SECRET` environment variable (minimum 32 bytes). Application must fail startup in production if missing or undersized.
- **Rate Limiting**: Apply rate-limiting filters/interceptors to `/api/auth/*` endpoints.

## Privacy & Leak Prevention
- **Logs**: Never log passwords, tokens, API keys, request bodies containing credentials, or full phone numbers (mask or exclude).
- **Environment**: Zero hardcoded secrets in codebase. Update `.env.example` when adding configuration keys.

## Multi-Tenancy & Authorization
- **Tenant Scope**: Business tables require a non-null, indexed `tenant_id`.
- **Identity Source**: Sourced strictly from authenticated `SecurityContext` via `TenantContext.getCurrentTenantId()`. Never accept tenant IDs from request bodies, parameters, or path headers.
- **Query Isolation**: Every repository query on business tables must explicitly filter by `tenantId`.
- **Cross-Tenant Response**: Return HTTP 404 (Not Found) on cross-tenant resource access, never 403 (prevents tenant resource enumeration).
- **Validation**: Enforce Bean Validation on all controller DTOs.
