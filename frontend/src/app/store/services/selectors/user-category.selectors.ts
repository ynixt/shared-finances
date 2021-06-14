import { Injectable } from '@angular/core';
import { Store, createSelector, createFeatureSelector } from '@ngrx/store';
import { EntityState } from '../../reducers';
import { UserCategoryState } from '../../reducers/user-category.reducer';

const userCategorySelector = createFeatureSelector<UserCategoryState>('userCategory');

export const getCategories = createSelector(userCategorySelector, (state: UserCategoryState) => state.categories);
export const getCategoriesState = createSelector(userCategorySelector, (state: UserCategoryState) => state);

@Injectable()
export class UserCategorySelectors {
  constructor(private store: Store<EntityState>) {}

  state$ = this.store.select(getCategoriesState);
  categories$ = this.store.select(getCategories);
}
