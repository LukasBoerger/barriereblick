# Barriereblick – Claude Code Kontext

Verbindliche Projektanweisungen: @AGENTS.md
Architektur und Datenmodell (verbindlich): @ai/architektur.md

## Coding-Regeln (ecc)

- Allgemein: @rules/ecc/common/coding-style.md, @rules/ecc/common/security.md,
  @rules/ecc/common/testing.md, @rules/ecc/common/git-workflow.md
- Java (`api/`): @rules/ecc/java/coding-style.md, @rules/ecc/java/security.md,
  @rules/ecc/java/patterns.md, @rules/ecc/java/testing.md
- Angular (`web/`): @rules/ecc/angular/coding-style.md, @rules/ecc/angular/patterns.md,
  @rules/ecc/angular/testing.md
- TypeScript (`worker/`): @rules/ecc/typescript/coding-style.md,
  @rules/ecc/typescript/testing.md

Bei Widersprüchen gilt: AGENTS.md und ai/architektur.md schlagen die ecc-Regeln.

## Arbeitsworkflow

Tasks liegen in `ai/tasks/`. Standardablauf pro Task:
architect → implementer → tester → reviewer (Definitionen in `.claude/agents/`).
Der "Nicht in Scope"-Block jedes Tasks ist verbindlich.

## Befehle

- API: `cd api && mvn verify`
- Web: `cd web && npm ci && ng build` (Tests: `ng test --watch=false`)
- Worker: `cd worker && npm ci && npm test` (Scan: `npm run scan -- <url>`)
