import { AuthSelectors } from './auth.selectors';
import { BankAccountSelectors } from './bank-account.selectors';

export * from './auth.selectors';
export * from './bank-account.selectors';

export const selectors = [AuthSelectors, BankAccountSelectors];
