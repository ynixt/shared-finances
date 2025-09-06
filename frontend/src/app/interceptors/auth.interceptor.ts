import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Observable, mergeMap, take } from 'rxjs';

import { KratosAuthService } from '../services/kratos-auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: KratosAuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!req.url.startsWith('/api')) {
      return next.handle(req);
    }

    return this.auth.token$.pipe(
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
