# Task: API-Grundgerüst mit JWT-Auth (M0-01)

## Ziel
Deploybares Spring-Boot-Gerüst in `api/` mit Multi-Tenant-Auth (JWT, Organisation + User),
als Fundament für alle weiteren Tasks. Kein Domänen-Feature, nur die Hülle.

## Kontext
- Neues Monorepo `bfsg-monitor`, Modul `api/`
- Referenz: JWT-Auth-Muster aus Kommuvo (SecurityConfig, JwtFilter, AuthController) –
  Dateien werden als Referenz bereitgestellt, NUR Auth + Org-Scoping übernehmen,
  nichts Domänenspezifisches (keine Termine, kein WhatsApp)
- Architektur-Referenz: `ai/architektur.md`, Abschnitte 2 (Komponenten) und 3 (Datenmodell)

## Anforderungen
1. Spring Boot 3.x, Java 21, Maven. Dependencies: Web, Security, Data JPA, PostgreSQL,
   Flyway, Validation, Lombok (falls in Kommuvo genutzt, konsistent bleiben)
2. Flyway-Migration `V1__init.sql` mit GENAU zwei Tabellen:
   - `organization` (id, name, logo_url, brand_color, plan, stripe_customer_id, created_at)
   - `app_user` (id, org_id FK, email unique, password_hash, role, created_at)
3. Entities + Repositories für beide Tabellen
4. Auth-Endpoints:
   - `POST /api/auth/register` → legt Organization + ersten User (role=ADMIN) an
   - `POST /api/auth/login` → liefert JWT (Claims: userId, orgId, role)
5. JWT-Filter + SecurityConfig: alles unter `/api/**` außer `/api/auth/**` erfordert Token
6. `GET /api/me` → liefert User + Organization (Smoke-Test für Auth)
7. Passwort-Hashing mit BCrypt, JWT-Secret aus Env-Variable `JWT_SECRET`,
   DB-Connection aus `DATABASE_URL`
8. CORS-Konfiguration für Vercel-Frontend (Origin aus Env-Variable `FRONTEND_ORIGIN`)
9. `application.yml` mit Profilen `local` und `prod`

## Akzeptanzkriterien
- [ ] `mvn verify` läuft grün
- [ ] Integrationstest: register → login → `GET /api/me` mit Token liefert 200,
      ohne Token 401
- [ ] Integrationstest: User von Org A kann keine Daten von Org B sehen
      (vorbereitender Test auf `GET /api/me`-Ebene)
- [ ] Flyway-Migration läuft auf leerer Postgres-DB durch (Testcontainers)
- [ ] Kein Endpoint außer `/api/auth/**` ohne Token erreichbar

## Nicht in Scope
- KEINE Tabellen für site, scan_run, finding etc. (kommt in M1)
- KEIN Passwort-Reset, keine E-Mail-Verifizierung, kein Refresh-Token
- KEINE Stripe-Logik (Spalte stripe_customer_id bleibt leer)
- KEIN Site-CRUD, kein Dashboard-Endpoint

## Agent-Reihenfolge
1. architect → Prüft Kommuvo-Auth-Referenz, plant Paketstruktur, identifiziert was
   übernommen vs. neu geschrieben wird
2. implementer → Setzt Anforderungen 1–9 um
3. tester → Schreibt die Integrationstests (Testcontainers für Postgres)
4. reviewer → Fokus: Security (JWT-Validierung, BCrypt-Stärke, Org-Scoping,
   keine Secrets im Code, CORS nicht auf *)

## Komplexität
**Hoch** (Security-Code) → Sonnet nutzen:
```
claude config set model claude-sonnet-4-6
```
WICHTIG: Nach Abschluss zusätzlich manuelles Review des Security-Codes durch Lukas.
