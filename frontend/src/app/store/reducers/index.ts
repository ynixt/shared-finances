import { ActionReducerMap } from '@ngrx/store';
import * as AuthReducer from './auth.reducer';
import * as BankAccountReducer from './bank-account.reducer';
import * as CreditCardReducer from './credit-card.reducer';
import * as UserCategoryReducer from './user-category.reducer';

export { AuthReducer, BankAccountReducer, CreditCardReducer, UserCategoryReducer };

export interface EntityState {
  auth: AuthReducer.AuthState;
  bankAccount: BankAccountReducer.BankAccountState;
  creditCard: CreditCardReducer.CreditCardState;
  userCategory: UserCategoryReducer.UserCategoryState;
}

export const reducers: ActionReducerMap<EntityState> = {
  auth: AuthReducer.reducer,
  bankAccount: BankAccountReducer.reducer,
  creditCard: CreditCardReducer.reducer,
  userCategory: UserCategoryReducer.reducer,
};
