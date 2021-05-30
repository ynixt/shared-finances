import { createAction, props } from '@ngrx/store';
import { User } from 'src/app/@core/models';
import { AuthType } from 'src/app/@core/models';

export const login = createAction('[Auth] LOGIN', props<{ authType: AuthType }>());
export const authError = createAction('[Auth] AUTH_ERROR', props<{ error: any }>());
export const authSuccess = createAction('[Auth] AUTH_SUCCESS', props<{ user: User }>());

export const getCurrentUser = createAction('[Auth] GET_CURRENT_USER');
