import { ActionReducerMap } from '@ngrx/store';
import * as AuthReducer from './auth.reducer';
import * as BankAccountReducer from './bank-account.reducer';
import * as CreditCardReducer from './credit-card.reducer';

export { AuthReducer, BankAccountReducer, CreditCardReducer };

export interface EntityState {
  auth: AuthReducer.AuthState;
  bankAccount: BankAccountReducer.BankAccountState;
  creditCard: CreditCardReducer.CreditCardState;
}

export const reducers: ActionReducerMap<EntityState> = {
  auth: AuthReducer.reducer,
  bankAccount: BankAccountReducer.reducer,
  creditCard: CreditCardReducer.reducer,
};
