# Scan-Worker

Laedt eine URL mit Playwright (headless Chromium), fuehrt axe-core aus und gibt das
Ergebnis als JSON auf stdout aus. Smoke-Test der Kernmechanik (M0-03) – kein Crawling,
keine DB, keine Queue.

> Das Ergebnis dokumentiert maschinell erkennbare Befunde. Es trifft keine Aussage
> ueber Rechtskonformitaet.

## Lokale Nutzung

```bash
npm install
npm run scan -- https://example.de
```

Nicht erreichbare oder ungueltige URLs fuehren zu Exit-Code 1 mit klarer Fehlermeldung.

## Docker-Nutzung

```bash
docker build -t barriereblick-worker .
docker run --rm -e SCAN_URL=https://example.de barriereblick-worker
```
