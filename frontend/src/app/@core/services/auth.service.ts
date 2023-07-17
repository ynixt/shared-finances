import {Injectable} from '@angular/core';
import {Auth, signInWithPopup, getAuth, UserCredential, GoogleAuthProvider} from '@angular/fire/auth';

import {AuthType, User} from '../models';
import {AuthDispatchers} from 'src/app/store';
import {Apollo, gql} from 'apollo-angular';
import {catchError, map} from 'rxjs/operators';
import {from, Observable, of} from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private auth: Auth, private authDispatchers: AuthDispatchers, private apollo: Apollo) {
  }

  public getCurrentUser(): Observable<User | null> {
    return this.apollo
      .query<{ user: User }>({
        query: gql`
          query GetUser {
            user {
              id
              name
              email
              photoURL
              bankAccounts {
                id
                name
                enabled
                displayOnGroup
              }
            }
          }
        `,
      })
      .pipe(map(result => (result.errors || result.data == null || result.data.user == null ? null : result.data.user)));
  }

  public login(type: AuthType): Observable<User | null> {
    return from(this._login(type)).pipe(
      catchError(error => {
        this.authDispatchers.logout();
        return of(error);
      }),
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
