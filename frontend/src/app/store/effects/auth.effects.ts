import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of, from } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import { AuthService } from 'src/app/@core/services';
import { AuthActions } from '../actions';

@Injectable({
  providedIn: 'root',
})
export class AuthEffects {
  constructor(private actions$: Actions, private authService: AuthService) {}

  isLogged$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.getCurrentUser),
      switchMap(() =>
        from(this.authService.getCurrentUser()).pipe(
          map(user => AuthActions.authSuccess({ user })),
          catchError(error => of(AuthActions.authError({ error }))),
        ),
      ),
    ),
  );

  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.login),
      switchMap(action =>
        from(this.authService.login(action.authType)).pipe(
          map(user => AuthActions.authSuccess({ user })),
          catchError(error => of(AuthActions.authError({ error }))),
        ),
      ),
    ),
  );
}
