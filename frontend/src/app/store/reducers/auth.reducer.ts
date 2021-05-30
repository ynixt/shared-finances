import { createReducer, Action, on, ActionReducer } from '@ngrx/store';
import { User } from 'src/app/@core/models';
import { AuthActions } from '../actions';

export type AuthState = {
  loading: boolean;
  error?: any;
  user?: User;
  done: boolean;
};

export const initialState: AuthState = {
  loading: false,
  done: false,
};

const loggedReducer: ActionReducer<AuthState, Action> = createReducer(
  initialState,
  on(AuthActions.login, _ => {
    return {
      ...initialState,
      loading: true,
    };
  }),
  on(AuthActions.authSuccess, (_, action) => {
    return {
      ...initialState,
      user: action.user,
      done: true,
    };
  }),
  on(AuthActions.authError, (_, action) => {
    return {
      ...initialState,
      error: action.error,
      done: true,
    };
  }),
);

export function reducer(state: AuthState, action: Action): AuthState {
  return loggedReducer(state, action);
}
