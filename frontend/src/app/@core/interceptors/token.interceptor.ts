import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AngularFireAuth } from '@angular/fire/auth';
import { from, Observable, of } from 'rxjs';
import { catchError, map, switchMap, take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class TokenInterceptor implements HttpInterceptor {
  constructor(private auth: AngularFireAuth) {}

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
    const user = await this.auth.user.pipe(take(1)).toPromise();

    return user?.getIdToken();
  }
}
