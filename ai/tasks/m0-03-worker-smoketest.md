# Task: Scan-Worker Smoke-Test (M0-03)

## Ziel
Minimaler Node.js-Worker in `worker/`, der EINE URL mit Playwright lädt, axe-core
ausführt und das Ergebnis ausgibt. Beweist die Kernmechanik des Produkts. Keine Pipeline,
keine DB, keine Queue.

## Kontext
- Monorepo `bfsg-monitor`, Modul `worker/`
- Architektur-Referenz: `ai/architektur.md`, Abschnitte 2 und 4
- Läuft später auf Railway im Playwright-Docker-Image

## Anforderungen
1. Node.js 22, plain JavaScript oder TypeScript (TS bevorzugt, tsx zum Ausführen)
2. Dependencies: `playwright`, `@axe-core/playwright`
3. Skript `src/scan.ts`: nimmt URL als CLI-Argument
   - Chromium headless starten, Seite laden (Timeout 30 s, `waitUntil: networkidle`)
   - axe-core ausführen
   - Ausgabe als JSON auf stdout: url, timestamp, Anzahl Violations gesamt,
     Violations gruppiert nach impact, pro Violation: ruleId, impact, description,
     betroffene Selektoren (max. 5 pro Regel), html-Snippet gekürzt auf 300 Zeichen
4. Sauberes Beenden (Browser schließen auch im Fehlerfall), Exit-Code 0 bei Erfolg,
   1 bei Fehler (Seite nicht erreichbar etc.) mit verständlicher Fehlermeldung
5. `Dockerfile` auf Basis `mcr.microsoft.com/playwright:v1.x-noble` (aktuelle Version),
   CMD führt scan.ts gegen eine per Env-Variable übergebene URL aus
6. README im worker/-Ordner: lokale Nutzung (`npm run scan -- https://example.de`)
   + Docker-Nutzung in je 2 Zeilen

## Akzeptanzkriterien
- [ ] `npm run scan -- https://example.com` liefert valides JSON mit Violations
- [ ] Nicht erreichbare URL → Exit-Code 1, klare Fehlermeldung, kein hängender Prozess
- [ ] Docker-Build läuft lokal durch, Container-Run scannt erfolgreich
- [ ] Kein Zombie-Chromium nach Lauf (Browser wird immer geschlossen)

## Nicht in Scope
- KEIN Crawling (nur die eine übergebene URL)
- KEINE Datenbank, keine Job-Queue, kein Polling
- KEIN WCAG/EN-301-549-Mapping (kommt in M3 als Stammdaten)
- KEINE PDF-Erzeugung
- KEIN robots.txt-Handling (kommt mit dem Crawler in M1)

## Agent-Reihenfolge
1. implementer → Umsetzung (architect hier unnötig, Scope ist trivial)
2. tester → Test mit Mock/lokaler HTML-Fixture (Playwright gegen file://-URL mit
   absichtlichen a11y-Fehlern: fehlendes alt, schlechter Kontrast)
3. reviewer → Fokus: Ressourcen-Handling (Browser-Cleanup), Timeout-Verhalten

## Komplexität
**Niedrig** → Haiku reicht:
```
claude config set model claude-haiku-4-5-20251001
```
Tipp nach Abschluss: Skript gegen 2–3 echte Websites lokaler Firmen laufen lassen –
die Ergebnisse sind Anschauungsmaterial für die LinkedIn-Rückfragen.
