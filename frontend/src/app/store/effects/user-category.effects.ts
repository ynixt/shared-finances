import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of, from } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import { UserCategoryService } from 'src/app/@core/services/user-category.service';
import { UserCategoryActions } from '../actions';

@Injectable({
  providedIn: 'root',
})
export class UserCategoryEffects {
  constructor(private actions$: Actions, private userCategoryService: UserCategoryService) {}

  getCategories$ = createEffect(() =>
    this.actions$.pipe(
      ofType(UserCategoryActions.getUserCategories),
      switchMap(() =>
        from(this.userCategoryService.watchCategories()).pipe(
          map(categories => UserCategoryActions.getUserCategoriesSuccess({ categories })),
          catchError(error => of(UserCategoryActions.getUserCategoriesError({ error }))),
        ),
      ),
    ),
  );
}
