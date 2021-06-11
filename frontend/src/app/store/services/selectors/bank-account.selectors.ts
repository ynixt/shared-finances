import { Injectable } from '@angular/core';
import { Store, createSelector, createFeatureSelector } from '@ngrx/store';
import { filter, take } from 'rxjs/operators';
import { BankAccount } from 'src/app/@core/models';
import { EntityState } from '../../reducers';
import { AuthState } from '../../reducers/auth.reducer';
import { BankAccountState } from '../../reducers/bank-account.reducer';

const bankAccountSelector = createFeatureSelector<AuthState>('bankAccount');

export const getState = createSelector(bankAccountSelector, (state: BankAccountState) => state);
export const getBankAccounts = createSelector(bankAccountSelector, (state: BankAccountState) => state.bankAccounts);

@Injectable()
export class BankAccountSelectors {
  constructor(private store: Store<EntityState>) {}

  state$ = this.store.select(getState);
  bankAccounts$ = this.store.select(getBankAccounts);

  public currentBankAccounts(): Promise<BankAccount[]> {
    return new Promise<BankAccount[]>(resolve => {
      this.state$
        .pipe(filter(state => state.done))
        .pipe(take(1))
        .subscribe(state => {
          resolve(state.bankAccounts);
        });
    });
  }
}
