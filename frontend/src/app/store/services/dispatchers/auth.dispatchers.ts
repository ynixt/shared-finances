import { Injectable } from '@angular/core';
import { AngularFireAuth } from '@angular/fire/auth';
import { Store } from '@ngrx/store';
import { AuthType, User } from 'src/app/@core/models';

import { AuthActions } from '../../actions';
import { EntityState } from '../../reducers';

@Injectable()
export class AuthDispatchers {
  constructor(private store: Store<EntityState>, private auth: AngularFireAuth) {}

  public loginByGoogle(): void {
    this.store.dispatch(AuthActions.login({ authType: AuthType.Google }));
  }

  public getCurrentUser(): void {
    this.store.dispatch(AuthActions.getCurrentUser());
  }

  public async logout(): Promise<void> {
    await this.auth.signOut();
    this.store.dispatch(AuthActions.authSuccess({ user: null }));
  }

  public userUpdated(newUser: User): void {
    this.store.dispatch(AuthActions.authSuccess({ user: newUser }));
  }
}
