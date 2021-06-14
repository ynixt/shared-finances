import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of, from } from 'rxjs';
import { switchMap, map, catchError, filter } from 'rxjs/operators';
import { AuthService } from 'src/app/@core/services';
import { AuthActions, BankAccountActions, CreditCardActions, UserCategoryActions } from '../actions';

@Injectable({
  providedIn: 'root',
})
export class AuthEffects {
  constructor(private actions$: Actions, private authService: AuthService) {}

  getCurrentUser$ = createEffect(() =>
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
          map(() => AuthActions.getCurrentUser()),
          catchError(error => of(AuthActions.authError({ error }))),
        ),
      ),
    ),
  );

  authSuccessCreditCard$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.authSuccess),
      filter(authState => authState.user != null),
      map(authState => CreditCardActions.getCreditCardsSuccess({ creditCards: authState.user.creditCards })),
      catchError(error => of(BankAccountActions.getBankAccountsError({ error }))),
    ),
  );

  authSuccessBankAccount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.authSuccess),
      filter(authState => authState.user != null),
      map(authState => BankAccountActions.getBankAccountsSuccess({ bankAccounts: authState.user.bankAccounts })),
      catchError(error => of(BankAccountActions.getBankAccountsError({ error }))),
    ),
  );

  authSuccessUserCategory$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.authSuccess),
      filter(authState => authState.user != null),
      map(authState => UserCategoryActions.getUserCategories({ userId: authState.user.id })),
      catchError(error => of(UserCategoryActions.getUserCategoriesError({ error }))),
    ),
  );
}
