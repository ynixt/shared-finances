import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { from, Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { Category } from 'src/app/@core/models';

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  constructor(private apollo: Apollo) {}

  newCategory(category: Partial<Category>): Observable<Category> {
    return this.apollo
      .mutate<{ newCategory: Category }>({
        mutation: gql`
          mutation($name: String!, $color: String!) {
            newCategory(name: $name, color: $color) {
              id
              name
              color
            }
          }
        `,
        variables: {
          name: category.name,
          color: category.color,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.newCategory),
      );
  }

  editCategory(category: Category): Observable<Category> {
    return this.apollo
      .mutate<{ editCategory: Category }>({
        mutation: gql`
          mutation($id: String!, $name: String!, $color: String!) {
            editCategory(id: $id, name: $name, color: $color) {
              id
              name
              color
            }
          }
        `,
        variables: {
          id: category.id,
          name: category.name,
          color: category.color,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.editCategory),
      );
  }

  getById(categoryId: string): Promise<Category> {
    return this.apollo
      .query<{ category: Category }>({
        query: gql`
          query category($categoryId: String!) {
            category(categoryId: $categoryId) {
              id
              name
              color
            }
          }
        `,
        variables: {
          categoryId,
        },
      })
      .pipe(
        map(result => result.data.category),
        take(1),
      )
      .toPromise();
  }

  deleteCategory(categoryId: string): Observable<boolean> {
    return this.apollo
      .mutate<{ deleteCategory: boolean }>({
        mutation: gql`
          mutation($categoryId: String!) {
            deleteCategory(categoryId: $categoryId)
          }
        `,
        variables: {
          categoryId,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.deleteCategory),
      );
  }
}
