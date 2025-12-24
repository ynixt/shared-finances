import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Observable, mergeMap, take } from 'rxjs';

import { TokenStateService } from '../services/token-state.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private tokenStateService: TokenStateService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if ((!req.url.startsWith('/api') && !req.url.startsWith('/private/external')) || req.url.startsWith('/api/open/auth/refresh')) {
      return next.handle(req);
    }

    return this.tokenStateService.token$.pipe(
      take(1),
      mergeMap(token => {
        if (!token) {
          return next.handle(req);
        }

        const authReq = req.clone({
          setHeaders: { Authorization: `Bearer ${token}` },
        });

        return next.handle(authReq);
      }),
    );
  }
}
