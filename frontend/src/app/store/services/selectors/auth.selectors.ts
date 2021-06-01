import { Injectable } from '@angular/core';
import { Store, createSelector, createFeatureSelector } from '@ngrx/store';
import { filter, take } from 'rxjs/operators';
import { User } from 'src/app/@core/models';
import { EntityState } from '../../reducers';
import { AuthState } from '../../reducers/auth.reducer';

const loggedSelector = createFeatureSelector<AuthState>('auth');

export const getUser = createSelector(loggedSelector, (state: AuthState) => state.user);

export const getAuthIsLogged = createSelector(loggedSelector, (state: AuthState) => state.user != null);

export const getAuthState = createSelector(loggedSelector, (state: AuthState) => state);

export const getAuthError = createSelector(loggedSelector, (state: AuthState) => state.error);

@Injectable()
export class AuthSelectors {
  constructor(private store: Store<EntityState>) {}

  isLogged$ = this.store.select(getAuthIsLogged);
  state$ = this.store.select(getAuthState);
  error$ = this.store.select(getAuthError);
  user$ = this.store.select(getUser);

  public async isLogged(): Promise<boolean> {
    const user = await this.currentUser();
    return user != null;
  }

  public currentUser(): Promise<User> {
    return new Promise<User>(resolve => {
      this.state$
        .pipe(filter(state => state.done))
        .pipe(take(1))
        .subscribe(state => {
          resolve(state.user);
        });
    });
  }

  public done(): Promise<boolean> {
    return new Promise<boolean>(resolve => {
      this.state$
        .pipe(filter(state => state.done))
        .pipe(take(1))
        .subscribe(state => {
          resolve(state.done);
        });
    });
  }
}
