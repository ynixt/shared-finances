import { Injectable } from '@angular/core';
import { Store, createSelector, createFeatureSelector } from '@ngrx/store';
import { EntityState } from '../../reducers';
import { AuthState } from '../../reducers/auth.reducer';
import { BankAccountState } from '../../reducers/bank-account.reducer';

const bankAccountSelector = createFeatureSelector<AuthState>('bankAccount');

export const getBankAccounts = createSelector(bankAccountSelector, (state: BankAccountState) => state.bankAccounts);

@Injectable()
export class BankAccountSelectors {
  constructor(private store: Store<EntityState>) {}

  bankAccounts$ = this.store.select(getBankAccounts);
}
