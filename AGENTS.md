# Barriereblick – Agent Instructions

## Projekt

Barriereblick ist ein Monitoring-SaaS für Webagenturen: kontinuierliche, automatisierte
Barrierefreiheits-Scans (axe-core) von Kundenwebsites, White-Label-Berichte,
Regression-Alerts. Architektur und Datenmodell: siehe `ai/architektur.md` (verbindlich).

WICHTIGE PRODUKTREGEL: Das Produkt behauptet NIEMALS Rechtskonformität. In UI-Texten,
Berichten, E-Mails und Code-Kommentaren immer "keine maschinell erkennbaren Fehler
gefunden" / "Monitoring & Dokumentation" – nie "BFSG-konform", "rechtssicher" o. Ä.

## Workspace-Struktur (Monorepo)

- `api/` – Spring Boot 3 / Java 21 Backend (REST, Auth, Scheduler, Postgres + Flyway)
- `web/` – Angular 19 Frontend (Dashboard)
- `worker/` – Node.js 22 / TypeScript Scan-Worker (Playwright + @axe-core/playwright)
- `ai/` – Architektur (`architektur.md`), Task-Files (`tasks/`), Briefing
- `rules/ecc/` – Coding-Regeln, werden über CLAUDE.md-Imports geladen

Bei Änderungen, die mehrere Module betreffen: API-Verträge, DTOs, Validierung,
Fehlerfälle und Tests auf allen betroffenen Seiten prüfen.

## Allgemeine Arbeitsweise

- Erst planen, dann Code ändern. Tasks aus `ai/tasks/` sind die Arbeitsgrundlage.
- Der Block "Nicht in Scope" in jedem Task-File ist verbindlich. Nichts darüber hinaus
  bauen, auch wenn es naheliegend erscheint.
- Keine großen Refactorings ohne Begründung. Kleine, nachvollziehbare Änderungen.
- Keine Secrets, Tokens, `.env`-Dateien, Zertifikate oder private Schlüssel lesen,
  verändern oder ausgeben.
- Kein `git push`, kein Deployment, keine produktionsnahen Änderungen ohne explizite
  Freigabe.
- Am Ende immer nennen: was geändert wurde, welche Tests sinnvoll sind, welche Risiken
  offen bleiben.

## Backend-Regeln (`api/`)

- Controller dünn, Businesslogik in Services, Repositories datenbanknah.
- DTOs, API-Modelle und Entities sauber trennen. Eingaben an API-Grenzen validieren.
- Multi-Tenancy: JEDE Query auf mandantenfähige Tabellen filtert über `org_id` aus dem
  JWT. Kein Endpoint darf Daten fremder Organisationen liefern – bei jedem neuen
  Endpoint einen Test dafür ergänzen.
- Fehler zentral behandeln, keine technischen Exceptions ans Frontend.
- Transaktionen bewusst setzen. DB-Änderungen nur per Flyway-Migration,
  rückwärtskompatibel.
- Tests für neue Businesslogik verpflichtend (JUnit, Mockito, Testcontainers).

## Frontend-Regeln (`web/`)

- Standalone Components, Observables, async pipe, `takeUntilDestroyed()`.
- Keine Angular Signals einführen (bewusste Projektentscheidung für Konsistenz).
- API-Zugriffe über Services kapseln. Komponenten klein, lesbar, testbar.
- Loading-, Empty- und Error-States immer berücksichtigen.
- Accessibility ist hier Produkt-DNA: semantisches HTML, Labels, Tastaturbedienung,
  Kontraste. Unser eigenes Dashboard muss bestehen, was wir bei anderen anmahnen.

## Worker-Regeln (`worker/`)

- Node.js 22, TypeScript (KEIN CommonJS-Zwang – ältere ecc-Hinweise dazu ignorieren).
- Browser-Ressourcen IMMER aufräumen (try/finally um Playwright-Kontexte), Timeouts
  setzen, klare Exit-Codes.
- Höflichkeit gegenüber gescannten Sites: Delays pro Domain, identifizierbarer
  User-Agent, robots.txt respektieren (ab M1).
- Keine ungekürzten HTML-Snippets persistieren (max. 300 Zeichen, Datensparsamkeit).
- Job-Claiming nur über `SELECT ... FOR UPDATE SKIP LOCKED` auf `scan_jobs`.

## API-Verträge (modulübergreifend)

Wenn api/ und web/ betroffen sind: Request-/Response-DTOs, Statuscodes, Fehlerfälle,
Validierungsfehler und Frontend-Modelle gegeneinander prüfen; Tests beidseitig vorschlagen.

## Teststrategie

- Backend: Unit-Tests für Businesslogik, Integrationstests (Testcontainers) für API/Repo.
- Frontend: Component-Tests für UI-Logik, Service-Tests für API-Handling.
- Worker: Tests gegen lokale HTML-Fixtures mit absichtlichen a11y-Fehlern.
- E2E nur für kritische Hauptflows (frühestens nach M2).

## Railway / Deployment

- Railway MCP nur zur Analyse (Projekte, Services, Deployments, Logs).
- Keine Deployments, keine Änderungen an Environment Variables ohne explizite Anweisung.
- Keine Environment Variables oder Secrets ausgeben.
- Bei Problemen: erst Logs und Deployment-Status analysieren, dann Hypothesen,
  dann Maßnahmen vorschlagen.

## Code-Review-Kriterien

Besonders achten auf: Security (v. a. Org-Scoping, JWT-Handling), Verständlichkeit,
Testbarkeit, Fehlerbehandlung, Ressourcen-Cleanup im Worker, API-Kompatibilität,
unnötige Komplexität, fehlende Edge Cases.
