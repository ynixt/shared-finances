import { ActionReducerMap } from '@ngrx/store';
import * as AuthReducer from './auth.reducer';
import * as BankAccountReducer from './bank-account.reducer';

export { AuthReducer, BankAccountReducer };

export interface EntityState {
  auth: AuthReducer.AuthState;
  bankAccount: BankAccountReducer.BankAccountState;
}

export const reducers: ActionReducerMap<EntityState> = {
  auth: AuthReducer.reducer,
  bankAccount: BankAccountReducer.reducer,
};
