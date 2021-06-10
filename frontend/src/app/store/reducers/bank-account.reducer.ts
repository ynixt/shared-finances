import { createReducer, Action, on, ActionReducer } from '@ngrx/store';
import { BankAccount } from 'src/app/@core/models';
import { BankAccountActions } from '../actions';

export type BankAccountState = {
  loading: boolean;
  error?: any;
  bankAccounts?: BankAccount[];
  done: boolean;
};

export const initialState: BankAccountState = {
  loading: false,
  done: false,
};

const bankAccountReducer: ActionReducer<BankAccountState, Action> = createReducer(
  initialState,
  on(BankAccountActions.bankAccountNameChanged, (state, action) => {
    const bankAccount = [...state.bankAccounts.filter(bankAccount => bankAccount.id === action.bankAccountId)][0];
    const bankAccounts = state.bankAccounts.filter(bankAccount => bankAccount.id !== action.bankAccountId);

    bankAccounts.push({ ...bankAccount, name: action.newName });

    return {
      ...initialState,
      loading: true,
      bankAccounts,
    };
  }),
  on(BankAccountActions.bankAccountRemoved, (state, action) => {
    const bankAccounts = state.bankAccounts.filter(bankAccount => bankAccount.id !== action.bankAccountId);

    return {
      ...initialState,
      loading: true,
      bankAccounts,
    };
  }),
  on(BankAccountActions.getBankAccountsSuccess, (_, action) => {
    return {
      ...initialState,
      bankAccounts: action.bankAccounts,
      done: true,
    };
  }),
  on(BankAccountActions.getBankAccountsError, (_, action) => {
    return {
      ...initialState,
      error: action.error,
      done: true,
    };
  }),
);

export function reducer(state: BankAccountState, action: Action): BankAccountState {
  return bankAccountReducer(state, action);
}
