import { AuthDispatchers } from './auth.dispatchers';
import { BankAccountDispatchers } from './bank-account.dispatchers';
import { CreditCardDispatchers } from './credit-card.dispatchers';

export * from './auth.dispatchers';
export * from './bank-account.dispatchers';
export * from './credit-card.dispatchers';

export const dispatchers = [AuthDispatchers, BankAccountDispatchers, CreditCardDispatchers];
