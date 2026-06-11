# BFSG-Monitor – Architektur (MVP)

> Stand: Juni 2026. Zielbild für das MVP eines Solo-Entwicklers, optimiert auf
> vorhandenen Stack (Spring Boot 3 / Java 21, Angular 19, PostgreSQL, Railway, Vercel)
> und minimale Betriebskomplexität. Bewusst KEIN Over-Engineering.

---

## 1. Anforderungen

### Funktional (MVP, in Prioritätsreihenfolge)

1. **F1 – Scan-Engine:** Website crawlen (Sitemap bevorzugt, sonst BFS), Seiten mit axe-core
   prüfen, Findings mit Regel, Schweregrad, Selektor und Kontext speichern.
2. **F2 – Multi-Site-Dashboard:** Ampel-Übersicht aller Sites einer Agentur, Drilldown
   Site → Scan-Historie → Seiten → Findings.
3. **F3 – White-Label-PDF-Bericht:** Deutsch, Agentur-Logo/-Farben, Findings gruppiert nach
   WCAG-Kriterium mit Zuordnung zu EN 301 549, plus dokumentierte manuelle Prüfungen.
4. **F4 – Regression-Alerts:** Vergleich Scan n vs. n−1; bei neuen Findings E-Mail an Agentur.
5. **F5 – Manuelle Prüf-Checkliste:** EN-301-549-Kriterien, die nicht automatisch prüfbar sind,
   als abhakbare Checkliste pro Site (Haftungs- und Berichts-Baustein).
6. **F6 – Generator Barrierefreiheitserklärung:** Formular → fertiger HTML/Markdown-Text.
7. **F7 – Abrechnung:** Stripe-Subscriptions, Staffel nach Site-Anzahl. (Nach Geld-Test.)

### Nicht-funktional

- **Betrieb:** 1 Person. Alles, was nachts kaputtgehen kann, minimieren.
- **Last (realistisch):** 30 Agenturen × 30 Sites × 50 Seiten/Scan × wöchentlich
  ≈ 45.000 Seiten-Scans/Woche ≈ 270/Std. gleichmäßig verteilt → ein einzelner Worker reicht lange.
- **Kosten:** Railway-Budget < 20–40 €/Monat im MVP.
- **Höflichkeit:** Wir scannen fremde (öffentliche) Sites → Rate-Limit pro Domain,
  robots.txt respektieren, identifizierbarer User-Agent mit Kontakt-URL.
- **DSGVO-Minimierung:** Keine Personendaten außer Agentur-Accounts. HTML-Snippets in Findings
  kurz halten (max. ~300 Zeichen), Scans nach 12 Monaten aggregieren/löschen.

---

## 2. High-Level-Architektur

```
                    Vercel                         Railway
              ┌──────────────┐      ┌─────────────────────────────────────┐
   Agentur ──▶│  Angular 19  │─────▶│  Spring Boot API (Java 21)          │
              │  (Dashboard) │ REST │  - Auth (JWT, multi-tenant)         │
              └──────────────┘      │  - Sites/Scans/Findings CRUD        │
                                    │  - Scheduler (Spring @Scheduled)    │
                                    │  - Alert-Versand (E-Mail)           │
                                    │  - Stripe-Webhooks                  │
                                    └──────┬──────────────────┬───────────┘
                                           │ JDBC             │ HTTP (intern)
                                    ┌──────▼──────┐    ┌──────▼───────────┐
                                    │ PostgreSQL  │◀───│ Scan-Worker      │
                                    │ (+ Flyway)  │poll│ Node.js          │
                                    │ jobs-Tabelle│    │ Playwright       │
                                    └─────────────┘    │ + @axe-core/     │
                                                       │   playwright     │
                                                       │ + HTML→PDF       │
                                                       └──────────────────┘
              E-Mail: Resend oder Postmark (Transaktions-Mails, Alerts)
              Storage für PDFs: Postgres bytea (MVP) → S3-kompatibel später
```

### Komponenten-Entscheidungen (mit Trade-offs)

| Entscheidung | Wahl | Begründung / Trade-off |
|---|---|---|
| Scan-Engine-Sprache | **Eigener Node.js-Worker** (Playwright + `@axe-core/playwright`) | Bestgepflegte axe-Integration lebt im JS-Ökosystem. Alternative (axe-core-maven-html mit Playwright Java) hielte alles in Java, hinkt aber bei Updates hinterher. Trade-off: zweite Sprache im Projekt – akzeptabel, Worker ist < 500 Zeilen. |
| Worker-Deployment | **Separater Railway-Service** | Headless Chromium ist speicherhungrig (512 MB–1 GB); getrennt vom API-Container crasht ein OOM nicht die API. |
| Job-Queue | **Postgres-Tabelle `scan_jobs` mit `SELECT … FOR UPDATE SKIP LOCKED`** | Kein Redis/RabbitMQ-Betrieb. Bewährtes Muster, reicht bis weit über 100 Agenturen. Trade-off: kein Pub/Sub – Worker pollt alle 10 s, völlig okay. |
| Scheduling | **Spring `@Scheduled`** legt fällige Jobs an (Sites mit `next_scan_at <= now()`) | Nur 1 API-Instanz im MVP → kein ShedLock nötig; nachrüstbar. |
| PDF-Erzeugung | **HTML-Template → Playwright `page.pdf()` im Worker** | Ein Rendering-Pfad für Web-Ansicht und PDF, White-Label per CSS-Variablen. Alternative (JasperReports/OpenPDF) = zweite Template-Welt. |
| Crawling | sitemap.xml zuerst, Fallback BFS bis `max_pages` (Default 50, Plan-abhängig) | Deterministisch, planbar, schützt vor Endlos-Crawls. |
| Auth | JWT + Org-Scoping wie in Kommuvo | Bekanntes Muster wiederverwenden. |
| Mandantentrennung | `org_id` auf jeder Tabelle + Service-Layer-Filter | Row-Level-Security in Postgres erst bei Bedarf. |

---

## 3. Datenmodell (Kern)

```
organization (Agentur)
  id, name, logo_url, brand_color, plan, stripe_customer_id, created_at

app_user
  id, org_id → organization, email, password_hash, role, created_at

site
  id, org_id, name, base_url, scan_frequency (weekly|daily),
  max_pages, next_scan_at, last_status (ok|warn|fail), created_at

scan_run
  id, site_id, started_at, finished_at, status (queued|running|done|error),
  pages_scanned, findings_total, findings_new, findings_fixed, score

page
  id, scan_run_id, url, http_status, scanned_at

finding
  id, scan_run_id, page_id, rule_id (axe), impact (critical|serious|moderate|minor),
  wcag_sc (z. B. "1.1.1"), en301549_clause (z. B. "9.1.1.1"),
  selector, html_snippet (max 300 Zeichen), help_text_de,
  fingerprint (hash aus rule+selector+url → für Regression-Vergleich)

scan_job   -- Queue-Tabelle
  id, site_id, type (scan|report), status (queued|running|done|error),
  locked_at, attempts, payload jsonb, created_at

manual_check_item   -- Stammdaten (Seed)
  id, en301549_clause, wcag_sc, title_de, description_de, category

manual_check_result
  id, site_id, item_id, status (passed|failed|n_a|todo), note,
  checked_by (user), checked_at

report
  id, site_id, scan_run_id, pdf bytea, created_at

accessibility_statement
  id, site_id, payload jsonb (Formulardaten), generated_html, updated_at
```

**Regression-Logik (F4):** `finding.fingerprint` = Hash(rule_id, normalisierter Selektor, URL).
Neue Findings = Fingerprints in Scan n, die in Scan n−1 fehlen; behobene umgekehrt.
Alert-Mail nur bei neuen Findings mit Impact ≥ serious (konfigurierbar).

**Mapping-Stammdaten:** statische Tabelle axe-Regel → WCAG-Erfolgskriterium → EN-301-549-Klausel
→ deutsche Beschreibung. Das ist Content-Arbeit (einmalig, ~90 axe-Regeln), kein Code –
und gleichzeitig der wichtigste Differenzierer gegenüber rohen axe-Reports.

---

## 4. Scan-Pipeline (Ablauf)

1. Scheduler (API) findet Sites mit `next_scan_at <= now()` → legt `scan_job` an,
   setzt `next_scan_at` fort.
2. Worker pollt `scan_jobs` (`FOR UPDATE SKIP LOCKED`), claimt Job.
3. Worker: robots.txt lesen → sitemap.xml laden (Fallback: BFS von base_url) →
   URL-Liste auf `max_pages` kappen.
4. Pro URL: Playwright lädt Seite (Timeout 30 s, 1–2 s Delay pro Domain),
   axe-core läuft, Violations werden normalisiert (Mapping anreichern, Snippet kürzen,
   Fingerprint berechnen) und gebatcht in Postgres geschrieben.
5. Abschluss: Aggregat auf `scan_run` (Totals, neu/behoben, Score), `site.last_status` setzen.
6. API-Hook: bei `findings_new > 0` Alert-Mail über Resend/Postmark.
7. Fehlerpfad: `attempts++`, Retry max. 2× mit Backoff, danach `status=error` + Anzeige im
   Dashboard („Site nicht erreichbar" ist für die Agentur selbst eine wertvolle Info).

---

## 5. API-Skizze (REST, Auswahl)

```
POST   /api/auth/login
GET    /api/sites                      Liste + Ampel-Status
POST   /api/sites                      {name, baseUrl, frequency}
GET    /api/sites/{id}/runs            Scan-Historie
GET    /api/runs/{id}/findings?impact=&page=
POST   /api/sites/{id}/scan            manueller Scan (Rate-Limit!)
GET    /api/sites/{id}/checklist       manuelle Prüf-Items + Status
PUT    /api/checklist/{resultId}       {status, note}
POST   /api/sites/{id}/report          erzeugt Report-Job → PDF
GET    /api/reports/{id}/download
POST   /api/sites/{id}/statement       Erklärung generieren
POST   /api/stripe/webhook
```

---

## 6. Bauplan (Phasen, passend zum Fahrplan im Briefing)

| Phase | Inhalt | geschätzter Aufwand |
|---|---|---|
| M0 | Repos, CI (GitHub Actions), Railway/Vercel-Setup, Auth-Gerüst aus Kommuvo-Mustern | 4–5 Std. |
| M1 | Worker: Crawl + axe + Persistenz; minimale Site-Verwaltung | 10–12 Std. |
| M2 | Dashboard: Ampel-Übersicht, Scan-Historie, Finding-Liste | 8–10 Std. |
| M3 | Mapping-Stammdaten DE + PDF-Bericht (White-Label) | 8–10 Std. |
| M4 | Regression-Alerts + manuelle Checkliste | 5–6 Std. |
| M5 | Erklärungs-Generator | 3–4 Std. |
| M6 | Stripe + Pläne/Limits (erst nach erfolgreichem Geld-Test) | 5–6 Std. |

Empfehlung: M0 kann sofort parallel zur Validierung laufen (risikofrei, wiederverwendbar).
M1+ erst nach GO-Signal aus den Agentur-Antworten.

---

## 7. Was wir bewusst NICHT bauen (MVP-Disziplin)

- Kein eigener Prüfregel-Code – axe-core ist die Engine, wir bauen Orchestrierung + Aufbereitung.
- Kein Overlay/„Fix-Widget" (rechtlich und fachlich umstritten, Reputationsrisiko).
- Keine Konformitätsbewertung/Score mit Rechtsanspruch – nur Befund-Dokumentation.
- Kein Multi-Region-Scanning, kein Echtzeit-Streaming, kein Kubernetes.
- Keine Browser-Extension, keine CI-Integration (späteres Upsell für Dev-Teams).

## 8. Revisit-Punkte (wenn es wächst)

- > ~100 Agenturen: Worker horizontal skalieren (Queue trägt das bereits), PDFs nach S3,
  ggf. dedizierte Scan-Knoten.
- Mehrere API-Instanzen: ShedLock für Scheduler.
- Finding-Volumen: Partitionierung von `finding` nach Monat, Aggregat-Tabellen fürs Dashboard.
- Feature-Kandidaten nach Traktion: CI-Integration (PR-Checks), Slack/Teams-Alerts,
  Kunden-Read-Only-Links, Englisch/EAA-Erweiterung für EU-Markt.
