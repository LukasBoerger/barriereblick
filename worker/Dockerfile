# Playwright-Image bringt Chromium + Systemabhaengigkeiten bereits mit.
FROM mcr.microsoft.com/playwright:v1.60.0-noble

WORKDIR /app

# Erst Manifeste fuer reproduzierbare, gecachte Installs.
COPY package.json package-lock.json ./
RUN npm ci

# Anschliessend Quellcode.
COPY tsconfig.json ./
COPY src ./src

# Browser sind im Basis-Image enthalten -> KEIN "playwright install".
# Die zu scannende URL kommt zur Laufzeit via SCAN_URL.
CMD ["npx", "tsx", "src/scan.ts"]
