import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { BankAccount } from 'src/app/@core/models';

import { BankAccountActions } from '../../actions';
import { EntityState } from '../../reducers';

@Injectable()
export class BankAccountDispatchers {
  constructor(private store: Store<EntityState>) {}

  public bankAccountNameChanged(bankAccountId: string, newName: string): void {
    this.store.dispatch(BankAccountActions.bankAccountNameChanged({ bankAccountId, newName }));
  }

  public bankAccountAdded(newBankAccount: BankAccount): void {
    this.store.dispatch(BankAccountActions.bankAccountAdded({ newBankAccount }));
  }

  public bankAccountRemoved(bankAccountId: string): void {
    this.store.dispatch(BankAccountActions.bankAccountRemoved({ bankAccountId }));
  }
}
