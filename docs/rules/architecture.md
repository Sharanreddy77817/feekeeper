# Architecture & Code Style Rules

## Layering & Structure
- **Package by Feature**: `auth`, `tenant`, `member`, `payment`, `dashboard`, `notification`, `common`.
- **Layers**: `controller` -> `service` -> `repository`.
- **DTOs**: Controllers use DTOs (Java records) only, never JPA entities.
- **Injection**: Constructor injection only; no field injection. Small focused methods. No unused code.

## Data & Domain Invariants
- **Database Migrations**: Schema changes strictly through new Flyway migrations (`V...__name.sql`). Never edit an applied migration. `ddl-auto` must be `validate`.
- **Currency & Money**: Always `BigDecimal`, SQL `numeric(12,2)`. Never `double` or `float`.
- **Timezone & Clock**: Use an injected `Clock` for all time logic. Business timezone is `Asia/Kolkata`.
- **Member Status**: Logic isolated in `MemberStatusCalculator`:
  - `OVERDUE`: `expiry < today`
  - `EXPIRING_SOON`: `today <= expiry <= today + 5 days`
  - `ACTIVE`: all other states.

## Error Handling & Web
- **Error Responses**: Handled centrally via `@RestControllerAdvice` producing RFC-style problem-detail JSON (no stack traces leaked).
- **Frontend**: Thymeleaf + Tailwind CSS (CDN) + vanilla JavaScript, mobile-first design. Always escape user-generated content.
