# Kommuvo Agent Instructions

## Workspace-Struktur

Dieses Workspace enthält zwei Repositories:

- `handwerkbot-java/`: Java Spring Boot Backend
- `handwerkbot-frontend/`: Angular Frontend

Arbeite immer mit Blick auf beide Repos. Wenn eine Änderung Frontend und Backend betrifft, prüfe API-Verträge, DTOs, Validierung, Fehlerfälle und Tests auf beiden Seiten.

## Allgemeine Arbeitsweise

- Erst planen, dann Code ändern.
- Keine großen Refactorings ohne Begründung.
- Keine Secrets, Tokens, `.env`-Dateien, Zertifikate oder private Schlüssel lesen, verändern oder ausgeben.
- Kein `git push`, kein Deployment und keine produktionsnahen Änderungen ohne explizite Freigabe.
- Kleine, nachvollziehbare Änderungen bevorzugen.
- Am Ende immer nennen:
    - was geändert wurde,
    - welche Tests sinnvoll sind,
    - welche Risiken offen bleiben.

## Backend-Regeln

Gilt für Dateien unter `handwerkbot-java/`.

Tech-Stack:
- Java
- Spring Boot
- JUnit
- Mockito
- relationale Datenbank

Regeln:
- Controller bleiben dünn.
- Businesslogik gehört in Services.
- Repository-Code bleibt datenbanknah und enthält keine komplexe Fachlogik.
- DTOs, API-Modelle und Entities sauber trennen.
- Eingaben an API-Grenzen validieren.
- Fehler zentral und nachvollziehbar behandeln.
- Keine technischen Exceptions ungefiltert an das Frontend geben.
- Transaktionen bewusst setzen.
- Tests für neue Businesslogik ergänzen.
- Bei Datenbankänderungen Migrationen und Rückwärtskompatibilität beachten.

## Frontend-Regeln

Gilt für Dateien unter `handwerkbot-frontend/`.

Tech-Stack:
- Angular
- TypeScript
- RxJS
- Angular Forms
- Jest oder Angular Test Utilities
- optional Cypress/Playwright für E2E

Regeln:
- Verwende Observables, async pipe und `takeUntilDestroyed()`.
- Keine Angular Signals vorschlagen oder einführen.
- API-Zugriffe über Services kapseln.
- Komponenten klein, lesbar und testbar halten.
- Loading-, Empty- und Error-States berücksichtigen.
- Keine verschachtelten Subscriptions ohne guten Grund.
- Keine Logik doppelt in mehreren Komponenten implementieren.
- Fehler im UI verständlich anzeigen.
- Accessibility beachten: Labels, Tastaturbedienung, ARIA nur wenn sinnvoll.

## API-Verträge

Wenn Backend und Frontend betroffen sind:

- Prüfe Request-/Response-DTOs.
- Prüfe Statuscodes.
- Prüfe Fehlerfälle.
- Prüfe Validierungsfehler.
- Prüfe, ob Frontend-Modelle zur Backend-API passen.
- Schlage Tests für beide Seiten vor.

## Teststrategie

Bei jeder Feature-Änderung prüfen:

Backend:
- Unit-Test für Businesslogik
- Integrationstest für API oder Repository, falls sinnvoll

Frontend:
- Component-Test für UI-Logik
- Service-Test für API-Handling
- E2E-Test nur für kritische Hauptflows

## Railway / Deployment-Regeln

- Railway MCP darf zur Analyse von Projekten, Services, Environments, Deployments und Logs genutzt werden.
- Keine Deployments ohne explizite Freigabe.
- Keine Environment Variables oder Secrets ausgeben.
- Environment Variables dürfen nur auf ausdrückliche Anweisung geändert werden.
- Bei Produktionsproblemen zuerst Logs und Deployment-Status analysieren, dann Hypothesen bilden, dann Maßnahmen vorschlagen.
- Vor jedem Deployment Build, Tests, Start-Command, Healthcheck und Risiken prüfen.

## Code-Review-Kriterien

Achte besonders auf:

- Security
- Verständlichkeit
- Testbarkeit
- Performance
- Fehlerbehandlung
- API-Kompatibilität
- unnötige Komplexität
- fehlende Edge Cases