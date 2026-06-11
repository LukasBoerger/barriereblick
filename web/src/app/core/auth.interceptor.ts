import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

/**
 * Haengt das Bearer-Token an alle API-Requests (ausser Login) und behandelt
 * 401 zentral: Token verwerfen + Redirect auf /login. Der Login-Call selbst
 * ist ausgenommen, damit die Login-Seite ihre Fehlermeldung anzeigen kann.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const isApiRequest = req.url.startsWith(environment.apiBaseUrl);
  const isLoginCall = req.url.startsWith(`${environment.apiBaseUrl}/api/auth/login`);

  let outgoing = req;
  if (isApiRequest && !isLoginCall && authService.token) {
    outgoing = req.clone({
      setHeaders: { Authorization: `Bearer ${authService.token}` },
    });
  }

  return next(outgoing).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && isApiRequest && !isLoginCall) {
        authService.logout(); // entfernt Token und leitet auf /login
      }
      return throwError(() => err);
    })
  );
};
