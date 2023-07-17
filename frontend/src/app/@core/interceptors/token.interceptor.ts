import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Auth } from '@angular/fire/auth';
import { from, Observable, of } from 'rxjs';
import { catchError, map, switchMap, take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class TokenInterceptor implements HttpInterceptor {
  constructor(private auth: Auth) {}

  public intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return from(this.getToken()).pipe(
      switchMap(token => {
        const requestClone =
          token == null
            ? request
            : request.clone({
                setHeaders: {
                  Authorization: `Bearer ${token}`,
                },
              });

        return next.handle(requestClone);
      }),
      catchError(err => {
        console.error(err);
        return next.handle(request);
      }),
    );
  }

  private async getToken(): Promise<string> {
    const user = await this.auth.currentUser;

    return user?.getIdToken();
  }
}
