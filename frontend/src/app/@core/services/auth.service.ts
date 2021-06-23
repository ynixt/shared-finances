import { Injectable } from '@angular/core';
import { AngularFireAuth } from '@angular/fire/auth';
import firebase from 'firebase/app';

import { AuthType, User } from '../models';
import { AuthDispatchers } from 'src/app/store';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { catchError, map, switchMap, take } from 'rxjs/operators';
import { from, Observable, of } from 'rxjs';
import { EmptyObject } from 'apollo-angular/types';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private auth: AngularFireAuth, private authDispatchers: AuthDispatchers, private apollo: Apollo) {}

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
                balance
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
    let userCredentials: firebase.auth.UserCredential;

    switch (type) {
      case AuthType.Google:
        userCredentials = await this.loginByGoogle();
        break;
    }
  }

  private async loginByGoogle(): Promise<firebase.auth.UserCredential> {
    return this.auth.signInWithPopup(new firebase.auth.GoogleAuthProvider());
  }
}
