# Task: Web-Grundgerüst mit Login (M0-02)

## Ziel
Minimales Angular-19-Gerüst in `web/`: Login-Seite, Auth-Guard, leeres Dashboard.
Beweist den Durchstich Frontend → API. Kein Design, kein Feature.

## Kontext
- Monorepo `bfsg-monitor`, Modul `web/`
- API aus M0-01 läuft lokal auf `http://localhost:8080`
- Muster aus Kommuvo-Frontend (AuthService, Interceptor, Guard) als Referenz nutzen
- Architektur-Referenz: `ai/architektur.md`, Abschnitt 2

## Anforderungen
1. Angular 19 Workspace (Standalone Components, kein NgModule-Setup)
2. `AuthService`: login(email, password) gegen `POST /api/auth/login`,
   Token-Haltung im Memory + localStorage, logout()
3. HTTP-Interceptor: hängt `Authorization: Bearer <token>` an alle API-Calls,
   bei 401 → Redirect auf /login
4. Routen: `/login` (öffentlich), `/dashboard` (geschützt durch Auth-Guard)
5. Login-Seite: E-Mail + Passwort, Fehleranzeige bei falschen Credentials,
   schlichtes Styling (kein Framework-Entscheid jetzt, reines CSS reicht)
6. Dashboard-Seite: zeigt nur "Eingeloggt als {email} ({orgName})" via `GET /api/me`
   + Logout-Button
7. `environment.ts` / `environment.prod.ts` mit `apiBaseUrl`

## Akzeptanzkriterien
- [ ] `ng build` läuft fehlerfrei
- [ ] Lokal: Login mit gültigen Credentials → Dashboard zeigt User-Daten
- [ ] Direktaufruf /dashboard ohne Token → Redirect /login
- [ ] 401 von API (abgelaufenes Token) → Redirect /login
- [ ] Logout entfernt Token und leitet auf /login

## Nicht in Scope
- KEIN UI-Framework-Setup (Material/Tailwind-Entscheidung kommt später)
- KEINE Registrierungs-Seite (Pilot-Accounts legen wir per API/SQL an)
- KEINE Site-Liste, keine Scan-Ansichten, keine Komponenten-Bibliothek
- KEIN State-Management-Framework

## Agent-Reihenfolge
1. architect → kurze Struktur-Skizze (Ordner, Services, Routen), max. 10 Min
2. implementer → Umsetzung
3. tester → Unit-Tests für AuthService + Guard (Token vorhanden/fehlt/abgelaufen)
4. reviewer → Fokus: Token-Handling (kein Token im URL, Interceptor-Logik, Guard-Lücken)

## Komplexität
**Niedrig** (Boilerplate nach bekanntem Muster) → Haiku reicht:
```
claude config set model claude-haiku-4-5-20251001
```
