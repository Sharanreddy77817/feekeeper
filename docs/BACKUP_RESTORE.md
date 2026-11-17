# PostgreSQL backup and restore

## Production backup policy

Use a managed PostgreSQL service with automated daily backups and point-in-time recovery when available. Configure:

- daily snapshots with at least 30 days of retention;
- continuous WAL/point-in-time recovery for the customer-facing database;
- encryption in transit and at rest;
- a backup region or account separate from the running application;
- alerts for failed or stale backups;
- restricted backup access with MFA and audited restore permissions.

A provider dashboard saying “backup enabled” is not proof of recoverability. Perform the restore test below at least quarterly and before major migrations. Record the date, backup identifier, restorer, duration, and result.

## Portable `pg_dump` backup

Use PostgreSQL client tools matching the server's major version. Keep credentials in environment variables or a protected password file, never command history.

```bash
export PGHOST="database-host"
export PGPORT="5432"
export PGDATABASE="feekeeper"
export PGUSER="backup_user"
export PGPASSWORD="read-from-secret-manager"

backup_file="feekeeper-$(date -u +%Y%m%dT%H%M%SZ).dump"
pg_dump --format=custom --compress=9 --no-owner --no-acl --file="$backup_file"
sha256sum "$backup_file" > "$backup_file.sha256"
```

Immediately move the dump and checksum to encrypted, access-controlled object storage. Apply retention/lifecycle rules and remove the local plaintext copy. Do not store dumps in Git, container images, application disks, or publicly readable buckets.

For the local Compose database:

```bash
docker compose exec -T db pg_dump --username="$DB_USER" --dbname=feekeeper --format=custom --no-owner --no-acl > feekeeper-local.dump
```

## Restore procedure

Never test a restore over the production database.

1. Select a backup and verify its checksum:

   ```bash
   sha256sum --check feekeeper-YYYYMMDDTHHMMSSZ.dump.sha256
   ```

2. Create an isolated PostgreSQL instance or empty database using the same PostgreSQL major version. Block public access.
3. Set connection variables for the isolated target, not production:

   ```bash
   export PGHOST="restore-test-host"
   export PGPORT="5432"
   export PGDATABASE="feekeeper_restore_test"
   export PGUSER="restore_operator"
   export PGPASSWORD="read-from-secret-manager"
   ```

4. Restore into the empty database:

   ```bash
   pg_restore --exit-on-error --no-owner --no-acl --dbname="$PGDATABASE" feekeeper-YYYYMMDDTHHMMSSZ.dump
   ```

5. Validate database structure and representative counts:

   ```bash
   psql --dbname="$PGDATABASE" --command="select version, description, success from flyway_schema_history order by installed_rank;"
   psql --dbname="$PGDATABASE" --command="select count(*) as tenants from tenants;"
   psql --dbname="$PGDATABASE" --command="select count(*) as members from members where deleted_at is null;"
   psql --dbname="$PGDATABASE" --command="select count(*) as payments from payments;"
   ```

6. Start a temporary FeeKeeper instance with `SPRING_PROFILES_ACTIVE=prod` and the isolated target's `DB_URL`, `DB_USER`, `DB_PASSWORD`, plus a temporary 32-byte-or-longer `JWT_SECRET`.
7. Confirm `/actuator/health/readiness` returns `UP`, login works for a designated test account, tenant isolation still returns `404`, recent payments and receipts are readable, and dashboard totals are plausible.
8. Confirm timestamp columns represent the same instants as production. Confirm business dates and scheduler boundaries display in `Asia/Kolkata`.
9. Record elapsed restore time and compare it with the recovery-time objective. Destroy the temporary application/database and securely delete downloaded backup material.

## Disaster restore

During a real incident, stop application writes, identify the approved recovery point, restore to a new managed database, run the validation steps, then switch `DB_URL` only after approval. Keep the old database read-only until reconciliation is complete. Rotate database credentials used during recovery and document any data-loss window.
