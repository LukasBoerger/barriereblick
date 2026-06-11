# BFSG-Monitor – Projekt-Briefing

> Arbeitstitel. Stand: Juni 2026. Dieses Dokument ist die zentrale Wissensbasis für das Projekt
> und dient als Projektkontext (Claude Project / Claude Code).

---

## 1. Produktidee in einem Satz

Ein White-Label-Monitoring-SaaS, mit dem **Webagenturen und Freelancer** die Barrierefreiheit
ihrer Kundenwebsites **dauerhaft** überwachen, Regressionen früh erkennen und ihre Prüfarbeit
gegenüber Endkunden mit deutschen, BFSG-bezogenen Berichten dokumentieren.

**Explizit nicht:** ein Einmal-Scanner für Endkunden (Markt überfüllt, kostenlos verfügbar)
und kein Versprechen von Rechtskonformität (Haftungsthema, siehe Abschnitt 6).

---

## 2. Marktkontext und Beweislage (Recherche Juni 2026)

### Warum jetzt (erzwungene Nachfrage)

- BFSG gilt seit **28.06.2025** für Websites, Shops und digitale Dienste im B2C-Bereich.
- Erste **Abmahnungen bereits sechs Wochen nach Inkrafttreten**; Anfang 2026 erste Bußgelder.
  Kosten pro Abmahnung ca. 3.500–20.000 €, Bußgelder bis 100.000 € möglich.
- Marktüberwachungsbehörden der Länder sind seit Anfang 2026 in der **aktiven Kontrollphase**.
- EU-Druck auf Deutschland (Fristsetzung Mai 2026); in der Branche wird mit einer
  **BFSG-Novelle** gerechnet – Tendenz eher Verschärfung/Klarstellung.
- Übergangsfrist für Bestandsdienste endet **28.06.2028** → eingebaute Nachfrage-Rampe.
- Erwartung in der Branche: ab H2 2026 systematisches Scannen durch Abmahnkanzleien.

### Warum Monitoring statt Audit

- Barrierefreiheit ist **kein einmaliges Projekt**: Redakteure laden Bilder ohne Alt-Texte hoch,
  Theme-/Plugin-Updates zerstören Kontraste und Fokus-Reihenfolgen.
- Automatisierte Tools (axe-core, WAVE, Lighthouse) finden nur einen **Teil** der
  WCAG-Verstöße (grob 30–50 %, abhängig von Site und Zählweise). Manuelle Prüfung bleibt nötig
  → bleibt bewusst Aufgabe der Agentur, unser Tool dokumentiert beides.

### Warum Agenturen als Kunden (nicht Site-Betreiber)

- Eine Agentur bringt 20–100 Sites auf einmal → wenige Kunden nötig.
- Agenturen verkaufen seit 2025 massenhaft BFSG-Audits → haben das **Folgeproblem**
  (Bestandskunden im Blick behalten, Arbeit dokumentieren, Erklärungen pflegen).
- Erreichbar ohne Kaltakquise-Schmerz: LinkedIn (BFSG-Poster!), Meetups (WordPress, Shopware,
  TYPO3), Agentur-Verzeichnisse, Content/SEO.
- White-Label-Bericht = die Agentur sieht gegenüber ihrem Kunden gut aus → starker Kaufgrund.

### Wettbewerb (Stand Recherche)

| Anbieter | Positionierung | Lücke für uns |
|---|---|---|
| Eye-Able (DE) | Overlay + Audit + Monitoring, eher Endkunden/Enterprise | Preis & Agentur-White-Label im SMB-Segment prüfen |
| Siteimprove / Acquia Optimize | Enterprise-Suiten, hohe Preise | Kleine Agenturen (2–15 MA) ausgeschlossen |
| Kostenlose Scanner (WAVE, axe, Lighthouse, Accessibility Checker etc.) | Einmal-Scans, ohne Historie/Multi-Site/Bericht | Kein Monitoring, kein White-Label, keine BFSG/EN-301-549-Aufbereitung auf Deutsch |
| Diverse Audit-Agenturen | Dienstleistung, kein Tool | Potenzielle Partner/Kunden, keine Konkurrenz |

**Offene Hausaufgabe (Woche 3–4):** Preise von Eye-Able & Co. für ~50 Sites recherchieren.
Hypothese: Lücke unterhalb ~200 €/Monat für Multi-Site-Monitoring mit White-Label.

---

## 3. Geschäftsmodell (Hypothese)

- **Abo pro Agentur**, gestaffelt nach Anzahl Sites, z. B.:
  - Starter: bis 10 Sites – ~29–49 €/Monat
  - Agentur: bis 25 Sites – ~79–99 €/Monat
  - Pro: bis 100 Sites – ~199 €/Monat
- Early-Bird für Pilotagenturen: 49–99 €/Monat pauschal, Preis lebenslang garantiert.
- Zahlungsabwicklung: Stripe (Erfahrung aus Kommuvo vorhanden).
- Ziel-Meilensteine: 3 zahlende Agenturen (Validierung) → 30 Agenturen (~2–4k MRR) →
  100+ Agenturen (tragfähiges Solo-Business).

---

## 4. Validierungsplan (vor dem Bauen!)

### Vorgehen: asynchrone LinkedIn-Ansprache

Zielgruppe finden:

1. **Heißeste Gruppe:** LinkedIn-Suche nach „BFSG" / „Barrierefreiheit Website",
   Filter „Beiträge", Zeitraum „letzter Monat" → wer postet, verkauft gerade Audits.
   Erst kurz inhaltlich kommentieren, dann Vernetzung + Nachricht.
2. **Klassische Suche:** „Inhaber Webagentur", „Geschäftsführer Digitalagentur",
   „WordPress Agentur", „Shopware Agentur", „TYPO3" – Fokus NRW, aber auch bundesweit.
3. **Ergänzend:** Mitglieder von WordPress-/Shopware-Meetups NRW, Verzeichnisse wie Sortlist.
   Zielgröße der Agenturen: 2–15 Mitarbeiter.

Ziel: 15–20 Kontaktaufnahmen → 8–10 verwertbare Antworten.

### Ansprache-Nachricht (Du-Form, asynchron)

> Hi [Name], ich bin Entwickler aus dem Münsterland und beschäftige mich gerade mit dem Thema
> BFSG bei Agenturen – nicht um was zu verkaufen, ich will erst mal verstehen, wie das in der
> Praxis läuft. Mich interessieren eigentlich nur drei Dinge:
>
> 1. Prüft ihr Kundenseiten nach dem ersten Barrierefreiheits-Check nochmal regelmäßig nach,
>    oder ist das Thema dann durch?
> 2. Womit prüft ihr überhaupt (Tools)?
> 3. Was nervt am ganzen BFSG-Thema am meisten?
>
> Ganz entspannt: Kurze Textantwort reicht völlig, Stichpunkte sind super, Sprachnachricht hier
> über LinkedIn geht auch, wenn dir das lieber ist. Würde mich freuen – und falls ich Rückfragen
> habe, meld ich mich einfach nochmal. Danke dir!

(Sie-Variante analog; LinkedIn-Sprachnachrichten nur in der Mobile-App, zwischen Kontakten, max. 60 s.)

### Vertiefende Fragen (nur als Rückfrage bei engagierten Antworten)

1. Wie viele Kundenwebsites betreut ihr laufend, auf welchen Systemen?
2. Wie dokumentiert ihr gegenüber dem Kunden, dass geprüft wurde? Wie entsteht die
   Barrierefreiheitserklärung?
3. Hatten Kunden schon Abmahnungen/Behördenpost? (→ Marketing-Gold)
4. Ist es vorgekommen, dass eine geprüfte Seite später wieder Mängel hatte? Wie gemerkt?
5. Angenommen, ein Dashboard scannt alle Kundensites wöchentlich, alarmiert bei Regressionen
   und erzeugt White-Label-Berichte – was müsste es können?
6. Was dürfte das pro Site/Monat kosten – ab welchem Preis wäre es ein No-Brainer?

### Auswertung

Pro Antwort notieren: Name, Agenturgröße, Antwort auf Frage 1 (Nachprüfung ja/nein),
genannte Tools, stärkstes Schmerz-Zitat, ggf. Preisnennung.

### Go/Kill-Kriterien (vorab festgelegt!)

- **GO:** ≥ 3 von 8 Antworten bestätigen, dass laufendes Monitoring/Dokumentation heute schlecht
  gelöst ist, UND ≥ 2 sagen sinngemäß „würde ich testen".
- **KILL:** Mehrheit prüft einmalig und sieht kein wiederkehrendes Problem, ODER Mehrheit nutzt
  bereits zufrieden ein Tool. → Idee verwerfen (< 10 Std. investiert), Alternative:
  Energieberater-Workflow-Idee (mit DSGVO-Vorbehalt) reaktivieren.

---

## 5. Fahrplan (neben 35-Std.-Job und Bachelorarbeit, ~5–8 Std./Woche)

| Phase | Zeitraum | Inhalt | Aufwand |
|---|---|---|---|
| 1 | Woche 1–2 | Zielpersonen finden, 15–20 Anfragen verschicken | ~4–5 Std. |
| 2 | Woche 3–4 | Antworten auswerten, Go/Kill-Entscheidung; parallel Wettbewerbs-Pricing | ~3 Std. |
| 3 | Woche 5–10 | MVP (nur bei GO) – siehe Architektur-Doku | ~35 Std. |
| 4 | Woche 8+ | Pilotagenturen auf Beta-Version lassen, Feedback einarbeiten | laufend |
| 5 | Woche 11–12 | Geld-Test: Early-Bird-Angebot, Ziel 2–3 zahlende Agenturen | ~3 Std. |
| 6 | nach Zahlung | AGB/Leistungsbeschreibung vom IT-Anwalt (~1.000–2.000 €), Stripe live | – |

Hinweis Priorität: Die Anfragen (Phase 1–2) sind wichtiger als Code. Architektur planen und
Grundgerüst aufsetzen ist okay, aber Feature-Entwicklung erst nach GO-Signal.

---

## 6. Rechtliche Leitplanken (Haftung)

**Grundsatz:** Das BFSG verpflichtet ausschließlich den Wirtschaftsakteur (Site-/Shop-Betreiber).
Abmahnungen und Bußgelder treffen nie den Tool-Anbieter. Unser Risiko ist rein **vertraglich**
(Kunde will Schaden abwälzen, weil Tool „grün" zeigte).

Schutzbausteine:

1. **Leistungsbeschreibung:** Wir verkaufen „automatisierte Prüfung maschinell erkennbarer
   Kriterien (WCAG/EN 301 549) + Monitoring + Dokumentation". Explizit: ersetzt keine manuelle
   Prüfung, keine Rechtsberatung, garantiert keine Konformität. „Grün" = „keine maschinell
   erkennbaren Fehler", niemals „BFSG-konform".
2. **AGB (B2B):** Haftung für einfache Fahrlässigkeit auf vertragstypischen, vorhersehbaren
   Schaden begrenzen, Höhe deckeln (z. B. Jahresgebühr). Vorsatz/grobe Fahrlässigkeit sind nicht
   ausschließbar – normal.
3. **Marketing-Sprache:** NIE „macht Ihre Website rechtssicher" / „BFSG-konform garantiert"
   (Haftung + UWG/irreführende Werbung). IMMER „Monitoring, Frühwarnung, Dokumentation".
4. **Später:** UG/GmbH als Haftungsschale, IT-Vermögensschadenhaftpflicht (wenige hundert €/Jahr).
5. **Erklärungs-Generator:** Dokumentengeneratoren sind laut BGH-„Smartlaw"-Urteil (2021) keine
   unerlaubte Rechtsdienstleistung; Hinweis „ersetzt keine Rechtsberatung" aufnehmen.
6. **Beta-Phase:** schriftlicher Hinweis „Beta, automatisierte Prüfung, ersetzt keine manuelle
   Prüfung und keine Rechtsberatung" reicht vor dem Anwaltstermin.

Produktseitiger Haftungsschutz: Die **manuelle Prüf-Checkliste** (Agentur dokumentiert eigene
manuelle Prüfungen im Tool) zementiert, dass die fachliche Beurteilung bei der Agentur liegt.

---

## 7. Positionierung & Messaging (Entwurf)

- Kernversprechen: „Behalte die Barrierefreiheit aller Kundenwebsites im Blick – und beweise
  deine Arbeit mit einem Bericht in deinem Branding."
- Zielgruppen-Ton: Entwickler-zu-Agentur, pragmatisch, kein Compliance-Bullshit-Bingo.
- Content-Schiene (später): „BFSG-Status Q4 2026", „Was automatische Scans finden – und was
  nicht", Stadt-/Branchen-Stichproben als Linkbait.

## 8. Offene Fragen / Risiken

- [ ] Pricing-Lücke unterhalb Enterprise real? (Eye-Able/Siteimprove-Preise für 50 Sites)
- [ ] Antwortquote der asynchronen Ansprache ausreichend? (sonst doch Telefonate)
- [ ] BFSG-Novelle beobachten (Anwendungsbereich könnte sich ändern – eher Chance als Risiko)
- [ ] Wie viel Konkurrenz entsteht parallel? (Markt-Timing spricht für Tempo bei Distribution)
- [ ] DSGVO-Restfläche klein halten: Scans speichern HTML-Snippets → Snippets minimal halten,
      Aufbewahrung begrenzen (siehe Architektur-Doku)
