import { ActionReducerMap } from '@ngrx/store';
import * as AuthReducer from './auth.reducer';

export { AuthReducer };

export interface EntityState {
  auth: AuthReducer.AuthState;
}

export const reducers: ActionReducerMap<EntityState> = {
  auth: AuthReducer.reducer,
};
