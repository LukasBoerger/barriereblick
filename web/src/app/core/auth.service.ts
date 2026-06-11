import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

import { LoginRequest, TokenResponse } from './models/auth.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiBaseUrl = environment.apiBaseUrl;

  private tokenSubject = new BehaviorSubject<string | null>(null);
  public token$ = this.tokenSubject.asObservable();

  constructor() {
    // Load token from localStorage on init
    const stored = localStorage.getItem('auth_token');
    if (stored) {
      this.tokenSubject.next(stored);
    }
  }

  login(email: string, password: string): Observable<TokenResponse> {
    const loginRequest: LoginRequest = { email, password };
    return this.http.post<TokenResponse>(`${this.apiBaseUrl}/api/auth/login`, loginRequest)
      .pipe(
        tap(response => {
          localStorage.setItem('auth_token', response.token);
          localStorage.setItem('token_expires_at', response.expiresAt);
          this.tokenSubject.next(response.token);
        }),
        catchError(err => {
          // Error will be handled by component
          return throwError(() => err);
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

    if (!token || !expiresAtStr) {
      return false;
    }

    const expiresAt = new Date(expiresAtStr).getTime();
    return expiresAt > Date.now();
  }
}
