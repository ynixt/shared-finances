import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AngularFireAuth } from '@angular/fire/auth';
import firebase from 'firebase/app';

import { AuthType, User } from '../models';
import { AuthDispatchers } from 'src/app/store';
import { Apollo, gql } from 'apollo-angular';
import { take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(
    private httpClient: HttpClient,
    private auth: AngularFireAuth,
    private authDispatchers: AuthDispatchers,
    private apollo: Apollo,
  ) {}

  public async getCurrentUser(): Promise<User | null> {
    try {
      const result = await this.apollo
        .query<{ user: User }>({
          query: gql`
            query GetUser {
              user {
                name
                email
                photoURL
              }
            }
          `,
        })
        .pipe(take(1))
        .toPromise();

      if (result.errors || result.data == null || result.data.user == null) {
        return null;
      }

      return result.data.user;
    } catch (err) {
      console.error(err);
      return null;
    }
  }

  public async login(type: AuthType): Promise<User | null> {
    let userCredentials: firebase.auth.UserCredential;

    switch (type) {
      case AuthType.Google:
        userCredentials = await this.loginByGoogle();
        break;
    }

    try {
      return this.getCurrentUser();
    } catch (err) {
      this.authDispatchers.logout();
      throw err;
    }
  }

  private async loginByGoogle(): Promise<firebase.auth.UserCredential> {
    return this.auth.signInWithPopup(new firebase.auth.GoogleAuthProvider());
  }
}
