# Implementierungsplan M0-01 – API-Grundgerüst mit JWT-Auth

> Architect-Plan, Stand 2026-06-11. Grundlage: `ai/tasks/m0-01-api-skeleton.md`,
> `ai/architektur.md` (Abschnitte 2+3), `AGENTS.md`.
> Hinweis: Die im Task referenzierten Kommuvo-Dateien (SecurityConfig, JwtFilter,
> AuthController) liegen NICHT im Repo. Geplant wird daher mit Spring-Security-6-
> Standardmustern statt Copy-Adapt. Das ist eine bewusste Abweichung vom Task-Text
> (Abschnitt 8, Entscheidung E1).

---

## 1. Maven-Setup (`api/pom.xml`)

- Parent: `spring-boot-starter-parent` **3.5.x** (aktuellste 3.5-Patch-Version beim
  Implementieren prüfen; Task fordert explizit 3.x, daher kein Spring Boot 4).
- `<java.version>21</java.version>`, Packaging jar, GroupId `de.barriereblick`,
  ArtifactId `api`.

Dependencies (Versionen über Boot-BOM gemanagt, keine manuellen Versionen außer wo nötig):

| Dependency | Zweck |
|---|---|
| `spring-boot-starter-web` | REST |
| `spring-boot-starter-security` | Security-Basis, BCrypt |
| `spring-boot-starter-oauth2-resource-server` | JWT-Validierung (Nimbus, siehe E2) |
| `spring-boot-starter-data-jpa` | Entities/Repositories |
| `spring-boot-starter-validation` | Bean Validation an API-Grenze |
| `org.postgresql:postgresql` (runtime) | JDBC-Treiber |
| `org.flywaydb:flyway-core` + `flyway-database-postgresql` | Migrationen |
| `spring-boot-starter-test` (test) | JUnit 5, AssertJ, Mockito, MockMvc |
| `spring-security-test` (test) | Security-Testsupport |
| `org.testcontainers:postgresql` + `junit-jupiter` (test) | Integrationstests |
| `spring-boot-testcontainers` (test) | `@ServiceConnection` |

**Entscheidung Lombok: NEIN.**
- DTOs werden Java-21-Records (kein Boilerplate-Problem).
- Nur 2 Entities im Task-Scope → handgeschriebene Getter/Setter sind überschaubar.
- Spart Annotation-Processor-/IDE-Kopplung; "konsistent mit Kommuvo" ist nicht
  prüfbar, da Referenz fehlt. KISS gewinnt.

**Entscheidung JWT-Library: `spring-boot-starter-oauth2-resource-server` (Nimbus), KEIN jjwt.**
- Validierung (Signatur, `exp`, Bearer-Extraktion, 401-Pfad) macht Spring Security
  selbst → kein handgeschriebener `OncePerRequestFilter`, also weniger eigener
  Security-Code, der im Review brennen kann.
- Token-Erzeugung über `NimbusJwtEncoder` mit `SecretKey` (HS256) – symmetrisches
  Secret aus `JWT_SECRET`, passt zu "minimale Betriebskomplexität" (kein Keypair-
  Management).
- Nimbus kommt transitiv mit dem Starter, keine zusätzliche Drittlib zu pflegen.

---

## 2. Paketstruktur

Basis-Package: `de.barriereblick.api`

```
de.barriereblick.api
├── ApiApplication.java
├── config/        SecurityConfig, JwtConfig (Encoder/Decoder + Secret-Validierung), CorsProperties
├── common/        GlobalExceptionHandler, ApiError (Fehler-DTO), RestAuthEntryPoint
├── auth/          AuthController, AuthService, TokenService, dto/ (Records)
├── org/           Organization (Entity), OrganizationRepository, PlanType (Enum)
└── user/          AppUser (Entity), UserRepository, UserRole (Enum),
                   MeController, MeService, dto/MeResponse
```

- Controller dünn (nur DTO-Mapping + Delegation), Logik in `AuthService`/`MeService`,
  Repositories reine Spring-Data-Interfaces.
- DTOs strikt von Entities getrennt: Entities verlassen nie den Service-Layer.
- Feature-Schnitt (auth/org/user) statt Layer-Schnitt – passend zu späteren Modulen
  (site, scan, finding in M1+).

---

## 3. Flyway `V1__init.sql`

Pfad: `api/src/main/resources/db/migration/V1__init.sql`. GENAU zwei Tabellen
gemäß `architektur.md` Abschnitt 3:

```sql
CREATE TABLE organization (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    logo_url            VARCHAR(1024),
    brand_color         VARCHAR(16),
    plan                VARCHAR(32)  NOT NULL DEFAULT 'FREE',
    stripe_customer_id  VARCHAR(255),            -- bleibt leer (Nicht in Scope)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id         UUID NOT NULL REFERENCES organization(id),
    email          VARCHAR(320) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(32)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_app_user_email ON app_user (lower(email));
CREATE INDEX ix_app_user_org_id ON app_user (org_id);
```

**Entscheidung ID-Typ: UUID (nicht bigserial).**
- IDs landen in JWT-Claims und später in URLs → UUIDs sind nicht enumerierbar
  (Multi-Tenant-SaaS: IDOR-Risiko bei sequentiellen IDs).
- `gen_random_uuid()` ist seit PG 13 nativ, keine Extension nötig.
- Trade-off (größerer Index) bei dieser Datenmenge irrelevant.
- E-Mail-Unique case-insensitive (`lower(email)`), Service normalisiert zusätzlich
  auf lowercase.

---

## 4. Auth-Design

### `POST /api/auth/register`
- Request-Record: `orgName`, `email` (@Email, @NotBlank), `password` (@Size(min=12))
- Ablauf in `AuthService.register(...)`, **`@Transactional`**:
  1. E-Mail normalisieren (trim + lowercase), Duplikat-Check → 409 Conflict
  2. `Organization` anlegen (plan=FREE, stripe_customer_id bleibt null)
  3. `AppUser` anlegen: role=ADMIN, `password_hash` = BCrypt
  4. Direkt JWT ausstellen → Response wie Login (spart dem Frontend einen Roundtrip)
- Response: `201` + `TokenResponse`

### `POST /api/auth/login`
- Request-Record: `email`, `password`
- User per E-Mail laden + `BCryptPasswordEncoder.matches()`. Bei unbekannter E-Mail
  UND falschem Passwort identische Antwort: `401` mit generischer Meldung
  (keine User-Enumeration über Login).
- Response: `200` + `TokenResponse { token, expiresAt }`

### JWT (TokenService)
- HS256, `iss=barriereblick`, `sub=userId`, Claims: `orgId`, `role`
- Gültigkeit: 24 h (Property `app.jwt.expiry`, konfigurierbar). Kein Refresh-Token
  (Nicht in Scope) → 24 h ist der pragmatische MVP-Kompromiss.

### `GET /api/me`
- Authentifiziert; `MeService` lädt User über `sub` aus dem validierten JWT und
  Organization über die **DB-Beziehung des Users** (nicht blind aus dem Claim) →
  Org-Scoping passiert serverseitig, Claim `orgId` ist nur Convenience für später.
- Response-Record `MeResponse`: `user { id, email, role }`,
  `organization { id, name, logoUrl, brandColor, plan }`.
  KEIN `password_hash`, KEIN `stripe_customer_id` im DTO.

---

## 5. Security-Konfiguration

`config/SecurityConfig`:
- `SessionCreationPolicy.STATELESS`, CSRF aus (token-basiert, kein Cookie-Auth)
- `permitAll`: `/api/auth/**`; ALLES andere `authenticated()` –
  `anyRequest().authenticated()` als Default-Deny (Akzeptanzkriterium 5)
- `oauth2ResourceServer(jwt)` mit `NimbusJwtDecoder.withSecretKey(...)` →
  ersetzt den handgeschriebenen JwtFilter (siehe E2)
- `JwtAuthenticationConverter`: Claim `role` → `ROLE_<role>`-Authority
- `BCryptPasswordEncoder` als Bean (Default-Strength 10; ausreichend, bewusst kein
  Custom-Tuning im MVP)

`config/JwtConfig`:
- Liest `JWT_SECRET` aus Env. **Startup-Validierung:** fehlt das Secret oder ist es
  < 32 Bytes (256 Bit), wirft die Bean-Erzeugung eine Exception mit klarer Meldung →
  App startet nicht. Kein Default-Secret im prod-Profil.

CORS:
- `CorsConfigurationSource`-Bean, erlaubte Origin EXAKT aus `FRONTEND_ORIGIN`
  (Property `app.cors.frontend-origin`), kein Wildcard, erlaubte Methoden
  GET/POST/PUT/DELETE, Header Authorization/Content-Type.

Fehlerbehandlung (`common/`):
- `GlobalExceptionHandler` (`@RestControllerAdvice`): Validation → 400 mit Feldfehlern,
  Duplikat-E-Mail → 409, Fallback → 500 mit generischer Meldung (Details nur ins Log,
  keine Stacktraces/SQL ans Frontend).
- `RestAuthEntryPoint` (AuthenticationEntryPoint) + AccessDeniedHandler: generisches
  JSON `{ "status": 401/403, "message": "..." }` ohne technische Details.
- Einheitliches Fehler-DTO `ApiError { status, message, fieldErrors? }` – das ist
  der API-Vertrag, auf den sich `web/` später verlässt.

---

## 6. Konfiguration (`application.yml`)

- `application.yml` (gemeinsam): App-Name, `app.jwt.expiry: 24h`, Flyway enabled,
  JPA `ddl-auto: validate` (Schema NUR per Flyway), `open-in-view: false`.
- Profil `local`: JDBC-URL `jdbc:postgresql://localhost:5432/barriereblick`
  (überschreibbar), `FRONTEND_ORIGIN`-Default `http://localhost:4200`,
  `JWT_SECRET`-Default ein klar als dev-only markierter 32+-Zeichen-Wert
  (NUR im local-Profil; prod hat keinen Fallback).
- Profil `prod`: `spring.datasource.url: ${DATABASE_URL}`,
  `app.cors.frontend-origin: ${FRONTEND_ORIGIN}`, `JWT_SECRET` ohne Default.
- Achtung Railway: `DATABASE_URL` muss im JDBC-Format
  (`jdbc:postgresql://host:port/db?user=...&password=...`) gesetzt werden –
  als Kommentar in der yml dokumentieren (Railway liefert nativ `postgres://`).

---

## 7. Dateiliste

### Produktion (`api/src/main/...`)
| Datei | Inhalt |
|---|---|
| `pom.xml` | Maven-Setup gemäß Abschnitt 1 |
| `resources/application.yml` | Gemeinsame Config + Profile `local`/`prod` (multi-document yml) |
| `resources/db/migration/V1__init.sql` | Migration gemäß Abschnitt 3 |
| `java/de/barriereblick/api/ApiApplication.java` | `@SpringBootApplication`-Einstieg |
| `config/SecurityConfig.java` | Filterchain (stateless, permitAll auth, Rest authenticated), CORS-Source, BCrypt-Bean |
| `config/JwtConfig.java` | `JwtEncoder`/`JwtDecoder`-Beans aus `JWT_SECRET`, Startup-Validierung ≥ 256 Bit |
| `common/ApiError.java` | Record: einheitliches Fehler-DTO |
| `common/GlobalExceptionHandler.java` | `@RestControllerAdvice`: 400/404/409/500-Mapping, generische Meldungen |
| `common/RestAuthEntryPoint.java` | 401/403 als generisches JSON ohne technische Details |
| `common/EmailAlreadyUsedException.java` | Fachliche Exception → 409 |
| `auth/AuthController.java` | Dünner Controller: `/api/auth/register`, `/api/auth/login` |
| `auth/AuthService.java` | Register (transaktional: Org + ADMIN-User), Login (BCrypt-Check) |
| `auth/TokenService.java` | JWT-Erzeugung (sub=userId, Claims orgId/role, exp) |
| `auth/dto/RegisterRequest.java` | Record mit Bean-Validation |
| `auth/dto/LoginRequest.java` | Record mit Bean-Validation |
| `auth/dto/TokenResponse.java` | Record: token, expiresAt |
| `org/Organization.java` | JPA-Entity gemäß Datenmodell |
| `org/OrganizationRepository.java` | `JpaRepository<Organization, UUID>` |
| `org/PlanType.java` | Enum (vorerst nur FREE) |
| `user/AppUser.java` | JPA-Entity, `org_id`-FK |
| `user/UserRepository.java` | `findByEmail(String)` (lowercase-normalisiert) |
| `user/UserRole.java` | Enum (vorerst nur ADMIN) |
| `user/MeController.java` | Dünner Controller: `GET /api/me` |
| `user/MeService.java` | Lädt User per JWT-`sub`, Org über DB-Relation |
| `user/dto/MeResponse.java` | Record: user- + organization-Teil, keine sensiblen Felder |

### Tests (`api/src/test/...`)
| Datei | Inhalt |
|---|---|
| `support/AbstractIntegrationTest.java` | Basis: `@SpringBootTest` + Testcontainers-Postgres (`@ServiceConnection`), Test-`JWT_SECRET` |
| `auth/AuthFlowIT.java` | register → login → `GET /api/me` = 200; ohne Token = 401; kaputter/abgelaufener Token = 401; prüft implizit Flyway auf leerer DB |
| `auth/OrgScopingIT.java` | Zwei Orgs registrieren; Token von User A liefert auf `/api/me` ausschließlich Org-A-Daten (vorbereitender Scoping-Test gem. Akzeptanzkriterium) |
| `auth/AuthServiceTest.java` | Unit (Mockito): Duplikat-E-Mail → Exception, BCrypt wird genutzt, Login mit falschem Passwort/unbekannter E-Mail liefert identisches Verhalten |
| `auth/TokenServiceTest.java` | Unit: Claims (sub/orgId/role) und Expiry korrekt gesetzt |
| `config/SecurityConfigIT.java` | Beliebiger Nicht-Auth-Pfad ohne Token → 401 (Default-Deny); CORS-Preflight nur für `FRONTEND_ORIGIN` |
| `config/JwtSecretValidationTest.java` | Secret fehlt / < 32 Bytes → Context-Start schlägt fehl |

Umsetzungsreihenfolge: pom → yml → Migration → Entities/Repos → JwtConfig/TokenService
→ SecurityConfig → AuthService/Controller → Me → Fehlerbehandlung → Tests
(tester-Agent vertieft die IT-Fälle).

---

## 8. Entscheidungen & Risiken

### Entscheidungen (inkl. Abweichungen)
- **E1 – Abweichung vom Task-Text:** Kommuvo-Referenzdateien existieren nicht im Repo.
  Statt Copy-Adapt: Spring-Security-6-Standardmuster. Kein fachlicher Verlust, da nur
  Auth + Org-Scoping übernommen werden sollte.
- **E2 – Resource-Server statt eigenem JwtFilter:** weniger handgeschriebener
  Security-Code, Spring übernimmt Bearer-Parsing/Validierung/401. Bewusst HS256 mit
  symmetrischem Secret (ein Env-Var, kein Keypair) – passt zu 1-Personen-Betrieb.
- **E3 – Kein Lombok:** Records + 2 kleine Entities, KISS.
- **E4 – UUID-PKs:** nicht enumerierbar (IDOR), nativ in PG, JWT-tauglich.
- **E5 – Register liefert direkt Token:** ein Endpoint-Roundtrip weniger; rein additiv
  zum geforderten Verhalten.
- **E6 – `/api/me` lädt Org über DB-Relation, nicht aus Claim:** Org-Scoping bleibt
  serverseitig verifiziert; Muster für alle M1+-Endpoints.

### Risiken / bewusste Lücken
- **Kein Rate-Limit auf `/api/auth/login`** (Brute-Force): Nicht im Task-Scope; die
  ecc-Security-Regel fordert es eigentlich → explizit als Folgearbeit markieren
  (spätestens vor öffentlichem Launch, z. B. Bucket4j). Abweichung dokumentiert.
- **User-Enumeration über Register (409 bei Duplikat):** bewusst akzeptiert –
  B2B-Produkt, UX schlägt hier das geringe Risiko; Login ist enumeration-sicher.
- **24h-Token ohne Revocation/Refresh:** MVP-Kompromiss (Refresh ist explizit
  Nicht-in-Scope); bei Leak hilft nur Secret-Rotation.
- **Railway-`DATABASE_URL`-Format** (postgres:// vs. jdbc:): Stolperfalle beim
  Deployment, in yml dokumentiert; betrifft M0-04/Deployment.
- **Testcontainers braucht Docker** auf der CI (relevant für M0-04, GitHub Actions
  hat Docker out-of-the-box).
- **Nicht in Scope eingehalten:** keine weiteren Tabellen, kein Refresh-Token, kein
  Passwort-Reset, keine Stripe-Logik (Spalte bleibt leer), kein Site-CRUD.
