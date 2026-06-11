import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MeResponse } from './models/user.model';
import { environment } from '../../environments/environment';

/** Kapselt GET /api/me – Komponenten greifen nie direkt auf HttpClient zu. */
@Injectable({ providedIn: 'root' })
export class MeService {
  private readonly http = inject(HttpClient);

  getMe(): Observable<MeResponse> {
    return this.http.get<MeResponse>(`${environment.apiBaseUrl}/api/me`);
  }
}
