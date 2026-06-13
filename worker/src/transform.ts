import type { RawScan } from "./axe-runner.js";
import type { ImpactKey, ScanOutput, ViolationOutput } from "./types.js";

const MAX_SELECTORS = 5;
const MAX_SNIPPET_LENGTH = 300;

const IMPACT_KEYS: ImpactKey[] = [
  "critical",
  "serious",
  "moderate",
  "minor",
  "unknown",
];

function normalizeImpact(impact: string | null | undefined): ImpactKey {
  switch (impact) {
    case "critical":
    case "serious":
    case "moderate":
    case "minor":
      return impact;
    default:
      return "unknown";
  }
}

/**
 * axe-core liefert pro betroffenem Knoten ein Selektor-"target", das selbst ein
 * Array (Shadow-DOM-Pfad) oder ein String sein kann. Wir flachen es zu einem
 * lesbaren CSS-Pfad ab.
 */
function selectorToString(target: unknown): string {
  if (Array.isArray(target)) {
    return target.map((part) => String(part)).join(" ");
  }
  return String(target);
}

function truncate(html: string): string {
  if (html.length <= MAX_SNIPPET_LENGTH) {
    return html;
  }
  return html.slice(0, MAX_SNIPPET_LENGTH);
}

/**
 * Reine Transformation: rohes axe-Result -> stabiles Output-Format.
 * Kein I/O, deterministisch, gut unit-testbar.
 */
export function toOutput(raw: RawScan): ScanOutput {
  const byImpact: Record<ImpactKey, number> = {
    critical: 0,
    serious: 0,
    moderate: 0,
    minor: 0,
    unknown: 0,
  };

  const violations: ViolationOutput[] = raw.results.violations.map((violation) => {
    const impact = normalizeImpact(violation.impact);
    byImpact[impact] += 1;

    const selectors = violation.nodes
      .slice(0, MAX_SELECTORS)
      .map((node) => selectorToString(node.target));

    const firstHtml = violation.nodes[0]?.html ?? "";

    return {
      ruleId: violation.id,
      impact,
      description: violation.description,
      selectors,
      htmlSnippet: truncate(firstHtml),
    };
  });

  // byImpact stabil ordnen (alle Keys vorhanden, deterministische Reihenfolge).
  const orderedByImpact = IMPACT_KEYS.reduce((acc, key) => {
    acc[key] = byImpact[key];
    return acc;
  }, {} as Record<ImpactKey, number>);

  return {
    url: raw.url,
    timestamp: raw.timestamp,
    violationsTotal: raw.results.violations.length,
    byImpact: orderedByImpact,
    violations,
  };
}
