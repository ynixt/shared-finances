import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { catchError, from, switchMap, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const apiAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isApiRequest =
        req.url.includes('/api') &&
        !req.url.includes('/api/users/current') &&
        !req.url.includes('/api/open/auth') &&
        !req.url.includes('/api/auth');

      const isUnauthorized = error.status === 401;

      if (isApiRequest && isUnauthorized) {
        return from(authService.loadUser()).pipe(
          switchMap(() => {
            return next(req);
          }),
          catchError(() => {
            return throwError(() => error);
          }),
        );
      }

      return throwError(() => error);
    }),
  );
};
