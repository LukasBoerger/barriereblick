import { describe, expect, it } from "vitest";
import { toOutput } from "../src/transform.js";
import type { RawScan } from "../src/axe-runner.js";

/**
 * Reine Unit-Tests gegen ein axe-Sample-JSON. Kein Browser, kein I/O.
 */

function makeRaw(violations: unknown[]): RawScan {
  return {
    url: "https://example.test/",
    timestamp: "2026-06-13T10:00:00.000Z",
    // Wir fuettern nur die Felder, die toOutput tatsaechlich liest.
    results: {
      violations,
    } as unknown as RawScan["results"],
  };
}

describe("toOutput", () => {
  it("maps basic fields and counts violations by impact", () => {
    const raw = makeRaw([
      {
        id: "image-alt",
        impact: "critical",
        description: "Images must have alternate text",
        nodes: [{ target: ["img"], html: "<img src=\"logo.png\">" }],
      },
      {
        id: "label",
        impact: "serious",
        description: "Form elements must have labels",
        nodes: [{ target: ["input"], html: "<input type=\"text\">" }],
      },
    ]);

    const out = toOutput(raw);

    expect(out.url).toBe("https://example.test/");
    expect(out.timestamp).toBe("2026-06-13T10:00:00.000Z");
    expect(out.violationsTotal).toBe(2);
    expect(out.byImpact.critical).toBe(1);
    expect(out.byImpact.serious).toBe(1);
    expect(out.byImpact.moderate).toBe(0);
    expect(out.violations[0].ruleId).toBe("image-alt");
    expect(out.violations[0].selectors).toEqual(["img"]);
  });

  it("treats missing/null impact as unknown", () => {
    const raw = makeRaw([
      {
        id: "some-rule",
        impact: null,
        description: "No impact set",
        nodes: [{ target: ["div"], html: "<div></div>" }],
      },
    ]);

    const out = toOutput(raw);

    expect(out.byImpact.unknown).toBe(1);
    expect(out.violations[0].impact).toBe("unknown");
  });

  it("limits selectors to a maximum of 5 per rule", () => {
    const nodes = Array.from({ length: 8 }, (_, i) => ({
      target: [`#node-${i}`],
      html: `<div id="node-${i}"></div>`,
    }));
    const raw = makeRaw([
      { id: "rule", impact: "minor", description: "d", nodes },
    ]);

    const out = toOutput(raw);

    expect(out.violations[0].selectors).toHaveLength(5);
    expect(out.violations[0].selectors[0]).toBe("#node-0");
  });

  it("truncates html snippet to 300 characters", () => {
    const longHtml = "<div>" + "x".repeat(500) + "</div>";
    const raw = makeRaw([
      {
        id: "rule",
        impact: "moderate",
        description: "d",
        nodes: [{ target: ["div"], html: longHtml }],
      },
    ]);

    const out = toOutput(raw);

    expect(out.violations[0].htmlSnippet).toHaveLength(300);
  });

  it("flattens array targets (shadow DOM paths) into a single string", () => {
    const raw = makeRaw([
      {
        id: "rule",
        impact: "serious",
        description: "d",
        nodes: [{ target: ["#host", "button"], html: "<button></button>" }],
      },
    ]);

    const out = toOutput(raw);

    expect(out.violations[0].selectors[0]).toBe("#host button");
  });

  it("returns zeroed byImpact and empty list when there are no violations", () => {
    const out = toOutput(makeRaw([]));

    expect(out.violationsTotal).toBe(0);
    expect(out.violations).toEqual([]);
    expect(out.byImpact).toEqual({
      critical: 0,
      serious: 0,
      moderate: 0,
      minor: 0,
      unknown: 0,
    });
  });
});

describe("toOutput edge cases", () => {
  it("treats undefined impact as unknown", () => {
    const raw = makeRaw([
      {
        id: "no-impact-field",
        // impact-Feld fehlt komplett (undefined)
        description: "Impact field absent",
        nodes: [{ target: ["span"], html: "<span></span>" }],
      },
    ]);

    const out = toOutput(raw);

    expect(out.violations[0].impact).toBe("unknown");
    expect(out.byImpact.unknown).toBe(1);
  });

  it("keeps an html snippet of exactly 300 characters untouched", () => {
    const exactHtml = "y".repeat(300);
    const raw = makeRaw([
      {
        id: "rule",
        impact: "minor",
        description: "d",
        nodes: [{ target: ["div"], html: exactHtml }],
      },
    ]);

    const out = toOutput(raw);

    expect(out.violations[0].htmlSnippet).toHaveLength(300);
    expect(out.violations[0].htmlSnippet).toBe(exactHtml);
  });

  it("truncates a 301-character snippet to exactly 300 characters", () => {
    const overHtml = "z".repeat(301);
    const raw = makeRaw([
      {
        id: "rule",
        impact: "minor",
        description: "d",
        nodes: [{ target: ["div"], html: overHtml }],
      },
    ]);

    const out = toOutput(raw);

    expect(out.violations[0].htmlSnippet).toHaveLength(300);
    expect(out.violations[0].htmlSnippet).toBe("z".repeat(300));
  });

  it("keeps exactly 5 selectors without capping", () => {
    const nodes = Array.from({ length: 5 }, (_, i) => ({
      target: [`#exact-${i}`],
      html: `<div id="exact-${i}"></div>`,
    }));
    const raw = makeRaw([
      { id: "rule", impact: "serious", description: "d", nodes },
    ]);

    const out = toOutput(raw);

    expect(out.violations[0].selectors).toHaveLength(5);
    expect(out.violations[0].selectors[4]).toBe("#exact-4");
  });

  it("yields an empty snippet when the first node has no html", () => {
    const raw = makeRaw([
      {
        id: "rule",
        impact: "moderate",
        description: "d",
        // node ohne html-Feld -> Snippet faellt auf "" zurueck
        nodes: [{ target: ["div"] }],
      },
    ]);

    const out = toOutput(raw);

    expect(out.violations[0].htmlSnippet).toBe("");
  });

  it("yields no selectors and empty snippet when a violation has no nodes", () => {
    const raw = makeRaw([
      { id: "rule", impact: "moderate", description: "d", nodes: [] },
    ]);

    const out = toOutput(raw);

    expect(out.violations[0].selectors).toEqual([]);
    expect(out.violations[0].htmlSnippet).toBe("");
    // Violation zaehlt trotzdem (es ist eine verletzte Regel).
    expect(out.violationsTotal).toBe(1);
    expect(out.byImpact.moderate).toBe(1);
  });

  it("counts multiple violations sharing the same impact", () => {
    const raw = makeRaw([
      {
        id: "rule-a",
        impact: "critical",
        description: "a",
        nodes: [{ target: ["a"], html: "<a></a>" }],
      },
      {
        id: "rule-b",
        impact: "critical",
        description: "b",
        nodes: [{ target: ["b"], html: "<b></b>" }],
      },
    ]);

    const out = toOutput(raw);

    expect(out.byImpact.critical).toBe(2);
    expect(out.violationsTotal).toBe(2);
  });
});
