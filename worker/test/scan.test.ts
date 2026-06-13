import { describe, expect, it } from "vitest";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { spawn } from "node:child_process";
import { createRequire } from "node:module";
import { runScan } from "../src/axe-runner.js";
import { toOutput } from "../src/transform.js";
import type { ImpactKey, ScanOutput } from "../src/types.js";

/**
 * Integrationstest: echtes Chromium gegen eine lokale file://-Fixture mit
 * absichtlichen a11y-Fehlern, plus Fehlerpfad fuer eine nicht erreichbare URL.
 *
 * file://-Pfade werden OS-neutral via pathToFileURL gebildet (Windows!).
 *
 * Der Fehlerpfad nutzt http://127.0.0.1:1/ statt einer .invalid-Domain: ein
 * reservierter, von Chromium als unsicher abgelehnter Port. Das wirft
 * deterministisch und wird NICHT von DNS-Captive-Portals abgefangen.
 */

const BROWSER_TEST_TIMEOUT_MS = 60_000;
const UNREACHABLE_URL = "http://127.0.0.1:1/";
const IMPACT_KEYS: ImpactKey[] = [
  "critical",
  "serious",
  "moderate",
  "minor",
  "unknown",
];

function fixtureUrl(name: string): string {
  return pathToFileURL(resolve(__dirname, "fixtures", name)).href;
}

const SCAN_ENTRY = resolve(__dirname, "..", "src", "scan.ts");

/**
 * Pfad zur tsx-CLI, robust ueber das bin-Feld der package.json aufgeloest
 * (statt einen internen dist-Pfad fest zu verdrahten).
 */
function tsxBinPath(): string {
  const require = createRequire(import.meta.url);
  const pkgPath = require.resolve("tsx/package.json");
  const pkg = require("tsx/package.json") as { bin: string };
  return resolve(pkgPath, "..", pkg.bin);
}

interface CliResult {
  code: number | null;
  stdout: string;
  stderr: string;
}

/**
 * Startet den CLI-Entry (src/scan.ts via tsx) als Subprozess und sammelt
 * stdout/stderr/Exit-Code. Dass der Prozess ueberhaupt terminiert, ist Teil
 * der Aussage (kein haengender Browser-Prozess).
 */
function runCli(url: string): Promise<CliResult> {
  return new Promise((resolveResult, rejectResult) => {
    const child = spawn(
      process.execPath,
      [tsxBinPath(), SCAN_ENTRY, url],
      { cwd: resolve(__dirname, ".."), env: process.env },
    );

    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => (stdout += String(chunk)));
    child.stderr.on("data", (chunk) => (stderr += String(chunk)));
    child.on("error", rejectResult);
    child.on("close", (code) => resolveResult({ code, stdout, stderr }));
  });
}

describe("runScan (integration)", () => {
  it(
    "detects intentional a11y violations in the local fixture",
    async () => {
      const raw = await runScan(fixtureUrl("a11y-broken.html"));
      const out = toOutput(raw);

      expect(out.violationsTotal).toBeGreaterThan(0);
      expect(out.violationsTotal).toBe(out.violations.length);

      const ruleIds = out.violations.map((v) => v.ruleId);
      // Fehlendes alt-Attribut ist ein zuverlaessig erkannter axe-Befund.
      expect(ruleIds).toContain("image-alt");

      // byImpact ist vollstaendig (alle Stufen vorhanden) und summiert sich
      // auf die Gesamtzahl der Violations.
      expect(Object.keys(out.byImpact).sort()).toEqual([...IMPACT_KEYS].sort());
      const impactSum = IMPACT_KEYS.reduce(
        (acc, key) => acc + out.byImpact[key],
        0,
      );
      expect(impactSum).toBe(out.violationsTotal);

      // Jede Violation traegt die vereinbarte Shape und haelt die Limits ein.
      for (const v of out.violations) {
        expect(typeof v.ruleId).toBe("string");
        expect(v.ruleId.length).toBeGreaterThan(0);
        expect(IMPACT_KEYS).toContain(v.impact);
        expect(typeof v.description).toBe("string");
        expect(v.htmlSnippet.length).toBeLessThanOrEqual(300);
        expect(v.selectors.length).toBeGreaterThan(0);
        expect(v.selectors.length).toBeLessThanOrEqual(5);
      }
    },
    BROWSER_TEST_TIMEOUT_MS,
  );

  it(
    "rejects when the target URL is unreachable",
    async () => {
      // Reservierter/unsicherer Port -> goto schlaegt deterministisch fehl,
      // ohne DNS-Aufloesung (kein Captive-Portal-Risiko).
      await expect(runScan(UNREACHABLE_URL)).rejects.toBeInstanceOf(Error);
    },
    BROWSER_TEST_TIMEOUT_MS,
  );
});

describe("scan CLI (integration)", () => {
  it(
    "exits 0 and prints valid JSON for a scannable URL",
    async () => {
      const result = await runCli(fixtureUrl("a11y-broken.html"));

      expect(result.code).toBe(0);

      const parsed = JSON.parse(result.stdout) as ScanOutput;
      expect(parsed.violationsTotal).toBeGreaterThan(0);
      expect(parsed.violations.map((v) => v.ruleId)).toContain("image-alt");
      expect(Object.keys(parsed.byImpact).sort()).toEqual(
        [...IMPACT_KEYS].sort(),
      );
    },
    BROWSER_TEST_TIMEOUT_MS,
  );

  it(
    "exits 1 with an error message for an unreachable URL",
    async () => {
      const result = await runCli(UNREACHABLE_URL);

      expect(result.code).toBe(1);
      expect(result.stdout).toBe("");
      expect(result.stderr).toContain(UNREACHABLE_URL);
      // Klare, nicht-technische Fehlermeldung an den Aufrufer.
      expect(result.stderr.toLowerCase()).toContain("fehlgeschlagen");
    },
    BROWSER_TEST_TIMEOUT_MS,
  );

  it(
    "exits 1 with usage hint when no URL is provided",
    async () => {
      // Kein URL-Argument und keine SCAN_URL-Env -> klare Nutzungsmeldung.
      const child = spawn(
        process.execPath,
        [tsxBinPath(), SCAN_ENTRY],
        {
          cwd: resolve(__dirname, ".."),
          env: { ...process.env, SCAN_URL: "" },
        },
      );

      const result = await new Promise<CliResult>((resolveResult) => {
        let stdout = "";
        let stderr = "";
        child.stdout.on("data", (chunk) => (stdout += String(chunk)));
        child.stderr.on("data", (chunk) => (stderr += String(chunk)));
        child.on("close", (code) => resolveResult({ code, stdout, stderr }));
      });

      expect(result.code).toBe(1);
      expect(result.stderr).toContain("Keine URL");
    },
    BROWSER_TEST_TIMEOUT_MS,
  );
});
