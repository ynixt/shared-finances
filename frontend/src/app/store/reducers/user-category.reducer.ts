import { createReducer, Action, on, ActionReducer } from '@ngrx/store';
import { Category } from 'src/app/@core/models';
import { UserCategoryActions } from '../actions';

export type UserCategoryState = {
  loading: boolean;
  error?: any;
  categories?: Category[];
  done: boolean;
};

export const initialState: UserCategoryState = {
  loading: false,
  done: false,
};

const loggedReducer: ActionReducer<UserCategoryState, Action> = createReducer(
  initialState,
  on(UserCategoryActions.getUserCategories, _ => {
    return {
      ...initialState,
      loading: true,
    };
  }),
  on(UserCategoryActions.getUserCategoriesSuccess, (_, action) => {
    return {
      ...initialState,
      categories: action.categories,
      done: true,
    };
  }),
  on(UserCategoryActions.getUserCategoriesError, (_, action) => {
    return {
      ...initialState,
      error: action.error,
      done: true,
    };
  }),
);

export function reducer(state: UserCategoryState, action: Action): UserCategoryState {
  return loggedReducer(state, action);
}
