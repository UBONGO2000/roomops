import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

const AUTH_ENDPOINTS = [
  `${environment.apiBaseUrl}/auth/login`,
  `${environment.apiBaseUrl}/auth/refresh`,
];

/**
 * Ajoute le token d'accès aux requêtes vers l'API (sauf login/refresh, pour éviter une boucle),
 * et tente un unique rafraîchissement automatique sur un 401 avant d'abandonner et de
 * déconnecter l'utilisateur.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthEndpoint = AUTH_ENDPOINTS.some((url) => req.url.startsWith(url));
  const isApiRequest = req.url.startsWith(environment.apiBaseUrl);

  const accessToken = authService.getAccessToken();
  const authorizedReq =
    !isAuthEndpoint && isApiRequest && accessToken
      ? req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
      : req;

  return next(authorizedReq).pipe(
    catchError((error: unknown) => {
      const canRetryWithRefresh =
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !isAuthEndpoint &&
        isApiRequest &&
        !!authService.getRefreshToken();

      if (!canRetryWithRefresh) {
        return throwError(() => error);
      }

      return authService.refreshAccessToken().pipe(
        switchMap((refreshed) => {
          const retriedReq = req.clone({
            setHeaders: { Authorization: `Bearer ${refreshed.accessToken}` },
          });
          return next(retriedReq);
        }),
        catchError((refreshError: unknown) => {
          authService.logout();
          router.navigateByUrl('/connexion');
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
