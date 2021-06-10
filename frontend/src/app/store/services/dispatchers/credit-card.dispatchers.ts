import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { CreditCard } from 'src/app/@core/models';

import { CreditCardActions } from '../../actions';
import { EntityState } from '../../reducers';

@Injectable()
export class CreditCardDispatchers {
  constructor(private store: Store<EntityState>) {}

  public creditCardAdded(newCreditCard: CreditCard): void {
    this.store.dispatch(CreditCardActions.creditCardAdded({ newCreditCard }));
  }

  public creditCardChanged(updatedCreditCard: CreditCard): void {
    this.store.dispatch(CreditCardActions.creditCardChanged({ updatedCreditCard }));
  }

  public creditCardRemoved(creditCardId: string): void {
    this.store.dispatch(CreditCardActions.creditCardRemoved({ creditCardId }));
  }
}
