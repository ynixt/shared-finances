import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AngularFireAuth } from '@angular/fire/auth';
import { from, Observable } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';

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
                  Authorization: token,
                },
              });

        return next.handle(requestClone);
      }),
    );
  }

  private async getToken(): Promise<string> {
    const user = await this.auth.user.pipe(take(1)).toPromise();

    return user?.getIdToken();
  }
}
