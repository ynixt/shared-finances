import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { catchError, from, switchMap, take, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { TokenStateService } from '../services/token-state.service';

export const apiAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const tokenStateService = inject(TokenStateService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isApiRequest =
        req.url.includes('/api') &&
        !req.url.includes('/api/users/current') &&
        !req.url.includes('/api/open/auth') &&
        !req.url.includes('/api/auth');

      const isUnauthorized = error.status === 401;

      if (!isApiRequest || !isUnauthorized) {
        return throwError(() => error);
      }

      return from(authService.loadUser()).pipe(
        switchMap(() => tokenStateService.token$.pipe(take(1))),
        switchMap(newToken => {
          if (newToken == null) {
            return throwError(() => error);
          }

          const authReq = req.clone({
            setHeaders: {
              Authorization: `Bearer ${newToken}`,
            },
          });

          return next(authReq);
        }),
        catchError(() => throwError(() => error)),
      );
    }),
  );
};
