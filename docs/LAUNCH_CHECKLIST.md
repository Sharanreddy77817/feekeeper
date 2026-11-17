# FeeKeeper launch checklist

## Hosting and network

- [ ] Production runs with `SPRING_PROFILES_ACTIVE=prod` behind HTTPS only.
- [ ] Custom domain DNS and TLS certificate are valid; HTTP redirects to HTTPS at the edge.
- [ ] Reverse proxy replaces, rather than appends untrusted, `X-Forwarded-*` headers before forwarding to FeeKeeper.
- [ ] PostgreSQL has no public ingress. Only the application and approved administrator network can connect.
- [ ] Host firewall exposes only HTTPS; application and database management ports are private.
- [ ] `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, and `/actuator/info` expose no sensitive details.
- [ ] `/actuator/metrics`, Swagger, and `/v3/api-docs` are not publicly accessible in production.

## Secrets and configuration

- [ ] `JWT_SECRET` is generated randomly, at least 32 bytes, stored in the host secret manager, and absent from Git/logs.
- [ ] JWT rotation has been tested and an owner understands that rotating it signs out existing sessions.
- [ ] Database and WhatsApp credentials are unique to production and have minimum required permissions.
- [ ] `CORS_ALLOWED_ORIGINS` contains only the exact production HTTPS origin(s), with no wildcard.
- [ ] `WHATSAPP_PROVIDER=mock` remains enabled until AiSensy/Interakt templates are approved and a controlled delivery test succeeds.
- [ ] OWASP Dependency-Check CI has a valid `NVD_API_KEY` secret and no unresolved CVSS 7+ finding.

## Data safety and operations

- [ ] Managed daily backups, retention, encryption, and point-in-time recovery are enabled.
- [ ] A restore test from [BACKUP_RESTORE.md](BACKUP_RESTORE.md) has passed and its recovery time is recorded.
- [ ] Health/readiness alerts, application error alerts, disk/database capacity alerts, and backup-failure alerts reach an owner.
- [ ] Production logs are JSON, include request IDs, and have verified retention/access controls.
- [ ] Logs have been sampled to confirm they contain no passwords, JWTs, API keys, request bodies, or full phone numbers.
- [ ] The scheduler runs at 10:00 Asia/Kolkata and only one instance sends reminders.

## Customer and legal readiness

- [ ] A first-customer tenant is created with verified business name, owner, phone, UPI ID, plan amounts, and durations.
- [ ] The customer knows how to add members, correct mistakes, record payments, download receipts, and use the manual WhatsApp fallback.
- [ ] Privacy policy and terms pages have been legally reviewed and published before public launch.
- [ ] A support contact, incident contact, refund/correction procedure, and data-deletion request process are documented.
- [ ] DPDP readiness has been reviewed for member names and phone numbers: clear notice and purpose, valid consent/other lawful basis where applicable, data minimization, retention/deletion schedule, access/correction process, processor contracts, reasonable security safeguards, and breach-response responsibilities.
- [ ] Cross-border processors and WhatsApp provider data flows are documented. Obtain qualified Indian legal advice; this checklist is operational guidance, not a legal opinion.
