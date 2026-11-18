# Testing Requirements & Standards

## Test Frameworks
- **Unit Testing**: JUnit 5 + Mockito for isolated unit logic.
- **Integration Testing**: Testcontainers (PostgreSQL container) for repository, multi-tenancy, and end-to-end integration flows.

## Mandatory Test Suites
- **Tenant Isolation**: Verify that Tenant A cannot read, mutate, or enumerate Tenant B's entities (must receive 404).
- **Payment & Mark-Paid**: Concurrency and pessimistic lock tests, idempotency key replay, and receipt sequence increments.
- **Scheduler & Deduplication**: Verify job selection boundaries and ensure no duplicate WhatsApp dispatches occur on the same day.
- **Status Calculation**: Boundary tests in `MemberStatusCalculator` for offset days `-1`, `0`, `5`, `6`.
- **Authentication**: JWT signing/verification, failed login generic errors, and rate-limiting limits.

## Verification Gate
- Do not delete, disable, or weaken existing tests to achieve a passing build.
- Full verification command before completion: `mvn clean verify`.
