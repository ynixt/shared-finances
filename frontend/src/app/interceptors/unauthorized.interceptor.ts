import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { catchError, from, switchMap, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

const AUTH_RETRY_CONTEXT = new HttpContextToken<boolean>(() => false);

export const apiAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isUnauthorized = error.status === 401;
      const alreadyRetried = req.context.get(AUTH_RETRY_CONTEXT);

      if (!isUnauthorized || !isEligibleForRefresh(req.url) || alreadyRetried) {
        return throwError(() => error);
      }

      return from(authService.refreshSessionFromUnauthorized()).pipe(
        switchMap(refreshedToken => {
          if (refreshedToken == null) {
            return throwError(() => error);
          }

          const authReq = req.clone({
            setHeaders: {
              Authorization: `Bearer ${refreshedToken}`,
            },
            context: req.context.set(AUTH_RETRY_CONTEXT, true),
          });

          return next(authReq);
        }),
        catchError(() => throwError(() => error)),
      );
    }),
  );
};

function isEligibleForRefresh(url: string): boolean {
  if (!url.includes('/api')) {
    return false;
  }

  if (url.includes('/api/open/auth')) {
    return false;
  }

  if (url.includes('/api/auth')) {
    return false;
  }

  if (url.includes('/api/users/current')) {
    return false;
  }

  return true;
}
