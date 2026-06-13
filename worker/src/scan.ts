import { runScan } from "./axe-runner.js";
import { toOutput } from "./transform.js";

/**
 * CLI-Entry. Bewusst duenn: URL-Quelle bestimmen, validieren, Scan ausfuehren,
 * Ergebnis ausgeben. Wir setzen process.exitCode und kehren zurueck, statt
 * process.exit() zu rufen: so flusht Node stdout/stderr vollstaendig, bevor der
 * Prozess endet (kein abgeschnittenes JSON in Pipes / Docker-Logs). Der Browser
 * wird in runScan() im finally geschlossen, daher haelt nichts die Event-Loop offen.
 */

function resolveUrl(): string | null {
  const fromArg = process.argv[2];
  const fromEnv = process.env.SCAN_URL;
  const candidate = fromArg ?? fromEnv;
  if (!candidate) {
    return null;
  }
  return candidate;
}

async function main(): Promise<void> {
  const candidate = resolveUrl();
  if (!candidate) {
    process.exitCode = 1;
    process.stderr.write(
      "Keine URL angegeben. Nutzung: npm run scan -- <url> (oder SCAN_URL setzen).\n",
    );
    return;
  }

  let url: string;
  try {
    url = new URL(candidate).href;
  } catch {
    process.exitCode = 1;
    process.stderr.write(`Ungueltige URL: ${candidate}\n`);
    return;
  }

  try {
    const raw = await runScan(url);
    const result = toOutput(raw);
    process.stdout.write(JSON.stringify(result, null, 2) + "\n");
  } catch (error) {
    process.exitCode = 1;
    const message = error instanceof Error ? error.message : String(error);
    process.stderr.write(`Scan fehlgeschlagen fuer ${url}: ${message}\n`);
  }
}

void main();
