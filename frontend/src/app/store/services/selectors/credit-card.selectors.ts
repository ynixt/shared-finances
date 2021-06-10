import { Injectable } from '@angular/core';
import { Store, createSelector, createFeatureSelector } from '@ngrx/store';
import { EntityState } from '../../reducers';
import { AuthState } from '../../reducers/auth.reducer';
import { CreditCardState } from '../../reducers/credit-card.reducer';

const creditCardSelector = createFeatureSelector<AuthState>('creditCard');

export const getCreditCards = createSelector(creditCardSelector, (state: CreditCardState) => state.creditCards);

@Injectable()
export class CreditCardSelectors {
  constructor(private store: Store<EntityState>) {}

  creditCards$ = this.store.select(getCreditCards);
}
