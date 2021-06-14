import { AuthEffects } from './auth.effects';
import { UserCategoryEffects } from './user-category.effects';

export * from './auth.effects';
export * from './user-category.effects';

export const effects: any[] = [AuthEffects, UserCategoryEffects];
