import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';

import { BankAccountActions } from '../../actions';
import { EntityState } from '../../reducers';

@Injectable()
export class BankAccountDispatchers {
  constructor(private store: Store<EntityState>) {}

  public bankAccountNameChanged(bankAccountId: string, newName: string): void {
    this.store.dispatch(BankAccountActions.bankAccountNameChanged({ bankAccountId, newName }));
  }

  public bankAccountRemoved(bankAccountId: string): void {
    this.store.dispatch(BankAccountActions.bankAccountRemoved({ bankAccountId }));
  }
}
