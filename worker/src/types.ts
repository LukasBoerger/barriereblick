/**
 * Output-Shape des Scan-Workers. Bewusst entkoppelt vom rohen axe-Result,
 * damit spaetere Pipeline-Schritte (M1+) ein stabiles Format konsumieren.
 */

export type ImpactKey = "critical" | "serious" | "moderate" | "minor" | "unknown";

export interface ViolationOutput {
  ruleId: string;
  impact: ImpactKey;
  description: string;
  /** Bis zu 5 CSS-Selektoren der betroffenen Knoten. */
  selectors: string[];
  /** HTML des ersten betroffenen Knotens, auf 300 Zeichen gekuerzt (Datensparsamkeit). */
  htmlSnippet: string;
}

export interface ScanOutput {
  url: string;
  timestamp: string;
  /** Anzahl verletzter Regeln (nicht Anzahl Knoten). */
  violationsTotal: number;
  /** Anzahl verletzter Regeln je Impact-Stufe. */
  byImpact: Record<ImpactKey, number>;
  violations: ViolationOutput[];
}
