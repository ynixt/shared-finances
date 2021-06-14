import { AuthSelectors } from './auth.selectors';
import { BankAccountSelectors } from './bank-account.selectors';
import { CreditCardSelectors } from './credit-card.selectors';
import { UserCategorySelectors } from './user-category.selectors';

export * from './auth.selectors';
export * from './bank-account.selectors';
export * from './credit-card.selectors';
export * from './user-category.selectors';

export const selectors = [AuthSelectors, BankAccountSelectors, CreditCardSelectors, UserCategorySelectors];
