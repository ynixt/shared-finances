import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Auth, user, User as FirebaseUser } from "@angular/fire/auth";
import { from, lastValueFrom, Observable } from "rxjs";
import { switchMap, take } from "rxjs/operators";

@Injectable({
  providedIn: "root"
})
export class TokenInterceptor implements HttpInterceptor {
  private user$: Observable<FirebaseUser>;

  constructor(private auth: Auth) {
    this.user$ = user(auth);
  }

  public intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return from(this.getToken()).pipe(
      switchMap(token => {
        const requestClone =
          token == null
            ? request
            : request.clone({
              setHeaders: {
                Authorization: `Bearer ${token}`
              }
            });

        return next.handle(requestClone);
      }),
    );
  }

  private async getToken(): Promise<string> {
    const user = await lastValueFrom(this.user$.pipe(take(1)).pipe(take(1)))

    return user?.getIdToken();
  }
}
