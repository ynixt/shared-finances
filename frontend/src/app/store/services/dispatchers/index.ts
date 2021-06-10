import { AuthDispatchers } from './auth.dispatchers';
import { BankAccountDispatchers } from './bank-account.dispatchers';

export * from './auth.dispatchers';
export * from './bank-account.dispatchers';

export const dispatchers = [AuthDispatchers, BankAccountDispatchers];
