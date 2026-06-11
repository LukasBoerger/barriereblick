# Plan: Web-Grundgerüst mit Login (M0-02)

## 1. Workspace-Erstellung

```bash
cd web
npx -y @angular/cli@19 new barriereblick \
  --standalone \
  --routing \
  --style=css \
  --skip-git \
  --package-manager npm
```

**Test-Runner:** Angular 19 Standard = Karma + Jasmine (in `angular.json` konfiguriert).
Test-Command: `ng test --watch=false` für CI, `ng test` für Entwicklung.

---

## 2. Ordnerstruktur

```
src/app/
├── core/
│   ├── auth.service.ts           # login(), logout(), token-Getter, isAuthenticated
│   ├── auth.interceptor.ts       # Bear-Token anhängen, 401-Handling
│   ├── auth.guard.ts             # Funktionaler Guard für /dashboard
│   └── models/
│       ├── auth.model.ts         # LoginRequest, TokenResponse DTOs
│       └── user.model.ts         # MeResponse (User + Organization)
├── pages/
│   ├── login/
│   │   ├── login.component.ts    # Standalone, Form-Binding, Fehler-Display
│   │   ├── login.component.css   # Minimales Styling (Button, Input, Layout)
│   │   └── login.component.html
│   ├── dashboard/
│   │   ├── dashboard.component.ts # Standalone, GET /api/me aufrufen, logout()
│   │   ├── dashboard.component.css
│   │   └── dashboard.component.html
│   └── not-found/
│       ├── not-found.component.ts # 404-Fallback
│       └── not-found.component.html
├── shared/
│   └── error.model.ts            # ErrorResponse (status, message, fieldErrors?)
├── app.routes.ts                 # Routen-Config mit Guard
├── app.config.ts                 # provideHttpClient(), Interceptor-Provider
└── main.ts

src/environments/
├── environment.ts                # apiBaseUrl: 'http://localhost:8080'
└── environment.prod.ts           # apiBaseUrl: 'https://api.barriereblick.de'
```

---

## 3. AuthService Design

**Ort:** `src/app/core/auth.service.ts`

```typescript
// Pseudo-Code
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private tokenSubject = new BehaviorSubject<string | null>(null);

  // Token von localStorage laden beim Service-Init
  constructor() {
    const stored = localStorage.getItem('auth_token');
    this.tokenSubject.next(stored ?? null);
  }

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/login', { email, password })
      .pipe(
        tap(response => {
          localStorage.setItem('auth_token', response.token);
          localStorage.setItem('token_expires_at', response.expiresAt);
          this.tokenSubject.next(response.token);
        }),
        catchError(err => {
          // Fehler durchreichen, Component zeigt Meldung
          throw err;
        })
      );
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('token_expires_at');
    this.tokenSubject.next(null);
    this.router.navigateByUrl('/login');
  }

  get token(): string | null {
    return this.tokenSubject.value;
  }

  get isAuthenticated(): boolean {
    const token = this.tokenSubject.value;
    const expiresAtStr = localStorage.getItem('token_expires_at');
    if (!token || !expiresAtStr) return false;
    
    // Einfache Prüfung: Token-Ablauf vs. now
    const expiresAt = new Date(expiresAtStr).getTime();
    return expiresAt > Date.now();
  }

  // Observable für Token-Änderungen (für Komponenten, die subscribe)
  token$: Observable<string | null> = this.tokenSubject.asObservable();
}
```

**localStorage-Strategie:**
- Key: `auth_token` (Token-String)
- Key: `token_expires_at` (ISO-String vom Server)
- Auf Expiry: Interceptor sieht 401 → Token löschen + Redirect /login
- XSS-Risiko: Nur mitigiert durch CSP (kommt später), aber localStorage-Ansatz ist Task-Vorgabe

---

## 4. HTTP-Interceptor (Funktional)

**Ort:** `src/app/core/auth.interceptor.ts`

```typescript
export function authInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const authService = inject(AuthService);
  const router = inject(Router);
  const environmentUrl = inject(ENVIRONMENT_URL); // wird in app.config bereitgestellt

  // Bearer-Header NUR anhängen, wenn:
  // 1. URL gehört zu apiBaseUrl (kein Cross-Origin-Token-Leak)
  // 2. Kein /api/auth/login-Call (Double-Token-Risiko unbedeutend, aber sauberer)
  const isApiRequest = req.url.startsWith(environmentUrl);
  const isLoginCall = req.url.includes('/api/auth/login');

  let modifiedReq = req;
  if (isApiRequest && !isLoginCall && authService.token) {
    modifiedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${authService.token}`
      }
    });
  }

  return next(modifiedReq).pipe(
    catchError(err => {
      // 401 → Token ungültig/abgelaufen
      if (err.status === 401) {
        authService.logout(); // löscht Token + redirect
      }
      return throwError(() => err);
    })
  );
}
```

**In app.config.ts:**
```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
    // ...
  ]
};
```

---

## 5. Funktionaler Guard

**Ort:** `src/app/core/auth.guard.ts`

```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated) {
    return true;
  }

  // Nicht authentifiziert → /login, mit return-URL als Query-Param
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
```

**In app.routes.ts:**
```typescript
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', component: NotFoundComponent }
];
```

---

## 6. DTOs (Angular Models)

**Ort:** `src/app/core/models/`

Auf Basis der API-DTOs (M0-01):

```typescript
// auth.model.ts
export interface TokenResponse {
  token: string;
  expiresAt: string; // ISO 8601 vom Server
}

// user.model.ts
export interface MeResponse {
  user: {
    id: string;
    email: string;
    role: string;
  };
  organization: {
    id: string;
    name: string;
    logoUrl: string;
    brandColor: string;
    plan: string;
  };
}

// error.model.ts
export interface ErrorResponse {
  status: number;
  message: string;
  fieldErrors?: Record<string, string[]>;
}
```

---

## 7. Komponenten

### LoginComponent
- **Pfad:** `src/app/pages/login/`
- **Binding:** email, password (FormControl oder zwei-Wege)
- **Submit:** `login()` aufrufen → authService.login() → bei Erfolg redirect /dashboard, bei Fehler error-Message zeigen
- **Accessibility:** `<label for="email">`, semantisches HTML, Tabulator-Navigation funktioniert
- **Loading-State:** `[disabled]` auf Button während Request läuft

### DashboardComponent
- **Pfad:** `src/app/pages/dashboard/`
- **Init:** `GET /api/me` aufrufen (via Service), User + Org anzeigen
- **Fehler:** bei API-Fehler (401 wird vom Interceptor gekocht) → Fallback-Meldung
- **Logout-Button:** ruft authService.logout() auf
- **async pipe:** `me$ | async` für Observable-Binding
- **takeUntilDestroyed():** automatisches Cleanup

---

## 8. Routen + Environments

**app.routes.ts:**
```typescript
const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', component: NotFoundComponent }
];
```

**environment.ts:**
```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080'
};
```

**environment.prod.ts:**
```typescript
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.barriereblick.de'
};
```

**app.config.ts:**
```typescript
import { ENVIRONMENT_URL } from './environments/environment.token';

export const appConfig: ApplicationConfig = {
  providers: [
    // ...
    { provide: ENVIRONMENT_URL, useValue: environment.apiBaseUrl }
  ]
};
```

---

## 9. Dateiliste

| Datei | Inhalt |
|-------|--------|
| `src/app/core/auth.service.ts` | Token-Verwaltung, login(), logout(), isAuthenticated |
| `src/app/core/auth.interceptor.ts` | Bearer-Header, 401-Handling |
| `src/app/core/auth.guard.ts` | Funktionaler Guard für protected Routes |
| `src/app/core/models/auth.model.ts` | TokenResponse DTO |
| `src/app/core/models/user.model.ts` | MeResponse DTO |
| `src/app/core/models/error.model.ts` | ErrorResponse DTO |
| `src/app/pages/login/login.component.ts` | Standalone, Form, Fehler-Display |
| `src/app/pages/login/login.component.html` | Email + Password Input, Submit-Button |
| `src/app/pages/login/login.component.css` | Minimales Styling |
| `src/app/pages/dashboard/dashboard.component.ts` | Standalone, GET /api/me, Logout-Button |
| `src/app/pages/dashboard/dashboard.component.html` | User-Info, Org-Name, Logout |
| `src/app/pages/dashboard/dashboard.component.css` | Einfaches Layout |
| `src/app/pages/not-found/not-found.component.ts` | 404-Fallback |
| `src/app/pages/not-found/not-found.component.html` | "Seite nicht gefunden" |
| `src/app/app.routes.ts` | Route-Konfiguration mit Guard |
| `src/app/app.config.ts` | HttpClient, Interceptor, Environment-Token |
| `src/app/main.ts` | bootstrapApplication mit appConfig |
| `src/environments/environment.ts` | apiBaseUrl = localhost:8080 |
| `src/environments/environment.prod.ts` | apiBaseUrl = produktions-URL |
| `src/environments/environment.token.ts` | InjectionToken für apiBaseUrl |

---

## 10. Build & Test

```bash
# Build
ng build

# Tests (Karma/Jasmine)
ng test --watch=false

# Dev-Server
ng serve
```

Lokal: `http://localhost:4200` → Login mit Test-User (aus M0-01) → Dashboard zeigt User-Daten.

---

## 11. Risiken & Entscheidungen

### Risiken

1. **localStorage XSS-Anfälligkeit**: Token in localStorage ist lesbar aus JS (XSS möglich).
   - **Mitigation:** CSP + HttpOnly-Cookies (kommt später), für MVP akzeptabel
   - **Task-Vorgabe:** localStorage, nicht Cookie

2. **Token-Expiry-Prüfung im Frontend:** Basiert auf `expiresAt`-Timestamp; bei Uhren-Skew kann Token-Ablauf nicht exakt sein.
   - **Mitigation:** Backend antwortet mit 401 → Interceptor fängt ab

3. **Guard-Lücke: Direct API-Calls ohne Route:** Wenn Komponente direkt `http.get()` aufruft, wird Guard umgangen.
   - **Mitigation:** AuthService ist zentral; alle API-Calls sollten über Service laufen. Interceptor fängt 401.

### Entscheidungen (vs. Architektur)

- **BehaviorSubject statt Signal:** AGENTS.md sagt "keine Signals", wir nutzen RxJS Observable + async pipe → Standard für Angular-Projekt
- **localStorage statt Cookies:** Task-Vorgabe, nicht von Architektur abweichend
- **Funktionale Interceptors & Guards:** Angular 19 Standard, keine NgModule-komplexität
- **Kein State-Management-Framework:** Task sagt "Nicht in Scope", einfache BehaviorSubject reicht fürs MVP

---

## Kontakt zur API (M0-01)

Aktualisierte API-Verträge aus `api/src/main/java/de/barriereblick/api/auth/dto/`:

- **POST /api/auth/login**
  - Request: `{ email: string, password: string }` (LoginRequest)
  - Response: 200 → `{ token: string, expiresAt: Instant }` (TokenResponse)
  - Response: 401 → `{ status: 401, message: "..." }`

- **GET /api/me** (mit `Authorization: Bearer <token>`)
  - Response: 200 → `{ user: { id, email, role }, organization: { id, name, logoUrl, brandColor, plan } }`
  - Response: 401 → Token ungültig/abgelaufen

---

## Implementierungs-Reihenfolge (für implementer Agent)

1. Angular-Workspace mit `ng new`
2. Core-Services: AuthService (login, logout, token-Verwaltung)
3. Guard + Interceptor
4. DTOs (Models)
5. Komponenten: Login, Dashboard, NotFound
6. Routen in app.routes.ts
7. Environment-Konfiguration
8. Unit-Tests für AuthService, Guard (Mockito-equivalent)
9. Build-Verifikation (`ng build`)
10. Dev-Test gegen lokale API

---

**Geschätzter Aufwand:** 8–10 Std. (Workspace + Services + Komponenten + Tests).
