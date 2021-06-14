import { AuthEffects } from './auth.effects';
import { CreditCardEffects } from './credit-card.effects';
import { UserCategoryEffects } from './user-category.effects';

export * from './auth.effects';
export * from './user-category.effects';
export * from './credit-card.effects';

export const effects: any[] = [AuthEffects, UserCategoryEffects, CreditCardEffects];
