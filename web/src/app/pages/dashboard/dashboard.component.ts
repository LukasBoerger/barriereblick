import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { AuthService } from '../../core/auth.service';
import { MeService } from '../../core/me.service';
import { MeResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent {
  private readonly meService = inject(MeService);
  private readonly authService = inject(AuthService);

  isLoading = true;
  errorMessage = '';

  // Genau EINE Subscription ueber die async pipe im Template.
  readonly me$: Observable<MeResponse> = this.meService.getMe().pipe(
    tap(() => (this.isLoading = false)),
    catchError((err: HttpErrorResponse) => {
      this.isLoading = false;
      // 401 behandelt der Interceptor (Redirect /login) – hier nur Anzeige.
      this.errorMessage =
        err.error?.message ?? 'Benutzerdaten konnten nicht geladen werden.';
      return EMPTY;
    })
  );

  onLogout(): void {
    this.authService.logout();
  }
}
