import { createAction, props } from '@ngrx/store';
import { BankAccount } from 'src/app/@core/models';

export const getBankAccountsError = createAction('[BankAccount] GET_BANK_ACCOUNTS_ERROR', props<{ error: any }>());
export const getBankAccountsSuccess = createAction('[BankAccount] GET_BANK_ACCOUNTS_SUCCESS', props<{ bankAccounts: BankAccount[] }>());

export const bankAccountRemoved = createAction('[BankAccount] BANK_ACCOUNT_REMOVED', props<{ bankAccountId: string }>());

export const bankAccountNameChanged = createAction(
  '[BankAccount] BANK_ACCOUNT_NAME_CHANGED',
  props<{ bankAccountId: string; newName: string }>(),
);
