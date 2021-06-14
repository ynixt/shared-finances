import { createAction, props } from '@ngrx/store';
import { Category } from 'src/app/@core/models';

export const getUserCategories = createAction('[USER_CATEGORY] GET_CATEGORIES');
export const getUserCategoriesError = createAction('[USER_CATEGORY] GET_CATEGORIES_ERROR', props<{ error: any }>());
export const getUserCategoriesSuccess = createAction('[USER_CATEGORY] AUTH_SUCCESS', props<{ categories: Category[] }>());
