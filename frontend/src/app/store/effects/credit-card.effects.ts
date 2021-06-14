import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of, from } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import { CreditCardService } from 'src/app/@core/services';
import { CreditCardActions } from '../actions';

@Injectable({
  providedIn: 'root',
})
export class CreditCardEffects {
  constructor(private actions$: Actions, private creditCardService: CreditCardService) {}

  getCategories$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CreditCardActions.getCreditCards),
      switchMap(() =>
        from(this.creditCardService.watchCreditCards()).pipe(
          map(creditCards => CreditCardActions.getCreditCardsSuccess({ creditCards })),
          catchError(error => of(CreditCardActions.getCreditCardsError({ error }))),
        ),
      ),
    ),
  );
}
