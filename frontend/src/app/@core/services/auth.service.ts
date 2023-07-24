import { Injectable } from "@angular/core";
import { Auth, signInWithPopup, getAuth, UserCredential, GoogleAuthProvider } from "@angular/fire/auth";

import { AuthType, User } from "../models";
import { AuthDispatchers } from "src/app/store";
import { catchError, map } from "rxjs/operators";
import { from, Observable, of } from "rxjs";
import { HttpClient } from "@angular/common/http";

@Injectable({
  providedIn: "root"
})
export class AuthService {
  constructor(private auth: Auth, private authDispatchers: AuthDispatchers, private httpClient: HttpClient) {
  }

  public getCurrentUser(): Observable<User | null> {
    return this.httpClient.get<User>("/api/auth/current-user");
  }

  public login(type: AuthType): Observable<User | null> {
    return from(this._login(type)).pipe(
      catchError(error => {
        this.authDispatchers.logout();
        return of(error);
      })
    );
  }

  private async _login(type: AuthType): Promise<void> {
    let userCredentials: UserCredential;

    switch (type) {
      case AuthType.Google:
        userCredentials = await this.loginByGoogle();
        break;
    }
  }

  private async loginByGoogle(): Promise<UserCredential> {
    return signInWithPopup(getAuth(), new GoogleAuthProvider());
  }
}
