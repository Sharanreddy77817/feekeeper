# Payment & Scheduler Rules

## Payment Processing (`mark-paid`)
- **Transaction & Locking**: Execute in a single transaction with a pessimistic lock on the target `Member` record (`SELECT ... FOR UPDATE`).
- **Idempotency**: Require and enforce `Idempotency-Key` header (scoped per tenant, max 100 characters). Replays return the recorded payment.
- **Expiry Computation**: `newExpiry = max(currentExpiry, today) + planDurationDays`.
- **Receipt Numbers**: Tenant-unique and strictly sequential per calendar year, generated via a pessimistically locked `ReceiptSequence` row.

## Scheduler & Notifications
- **Distributed Locking**: Daily 10:00 IST job must execute with ShedLock backed by PostgreSQL.
- **Deduplication**: Prevent duplicate reminder dispatches using `lastReminderSentDate`, updated exclusively upon confirmed provider send.
- **Fault Tolerance**: A failure on any single member must not abort the overall batch.
- **WhatsApp Providers**: Provider selected via `WHATSAPP_PROVIDER` (`mock` | `aisensy` | `interakt` | `meta`). Default is `mock`.
