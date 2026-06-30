import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.accessToken();
  const authenticatedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authenticatedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthEndpoint = request.url.includes('/api/auth/');
      if (error.status !== 401 || isAuthEndpoint || !token) return throwError(() => error);

      return auth.refreshAccessToken().pipe(
        switchMap(() => {
          const refreshedToken = auth.accessToken();
          return next(request.clone({ setHeaders: { Authorization: `Bearer ${refreshedToken}` } }));
        }),
        catchError((refreshError) => {
          auth.clearSession();
          void router.navigateByUrl('/connexion');
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
