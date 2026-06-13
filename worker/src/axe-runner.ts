import { chromium } from "playwright";
import { AxeBuilder } from "@axe-core/playwright";
import type { AxeResults } from "axe-core";

const PAGE_TIMEOUT_MS = 30_000;
// Obere Schranke fuer den gesamten Scan (Browser-Arbeit). Faengt ab, wenn axe-core
// analyze() oder ein anderer Schritt nach dem goto-Timeout haengt -> kein Zombie-Prozess.
const OVERALL_TIMEOUT_MS = 60_000;

/**
 * Identifizierbarer User-Agent mit Kontakt-URL (Hoeflichkeit gegenueber gescannten Sites).
 */
const USER_AGENT =
  "BarriereblickBot/0.1 (+https://barriereblick.de/bot) Playwright Chromium";

export interface RawScan {
  url: string;
  timestamp: string;
  results: AxeResults;
}

/**
 * Begrenzt ein Promise auf eine maximale Laufzeit. Bei Ablauf wird rejected;
 * der aufrufende finally-Block raeumt die Browser-Ressourcen auf.
 */
function withTimeout<T>(promise: Promise<T>, ms: number, label: string): Promise<T> {
  let timer: ReturnType<typeof setTimeout>;
  const timeout = new Promise<never>((_, reject) => {
    timer = setTimeout(
      () => reject(new Error(`${label} nach ${ms} ms abgebrochen (Timeout).`)),
      ms,
    );
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

async function scanWithBrowser(
  browser: import("playwright").Browser,
  url: string,
): Promise<RawScan> {
  const context = await browser.newContext({ userAgent: USER_AGENT });
  const page = await context.newPage();
  await page.goto(url, { waitUntil: "networkidle", timeout: PAGE_TIMEOUT_MS });

  const results = await new AxeBuilder({ page }).analyze();

  return {
    url,
    timestamp: new Date().toISOString(),
    results,
  };
}

/**
 * Laedt eine einzelne URL in headless Chromium und fuehrt axe-core aus.
 * Der Browser wird in jedem Fall geschlossen (try/finally), kein Zombie-Prozess.
 * Ein Gesamt-Timeout verhindert ein unbegrenztes Haengen nach erfolgreichem Start.
 */
export async function runScan(url: string): Promise<RawScan> {
  const browser = await chromium.launch({ headless: true });
  try {
    return await withTimeout(
      scanWithBrowser(browser, url),
      OVERALL_TIMEOUT_MS,
      "Scan",
    );
  } finally {
    await browser.close();
  }
}
