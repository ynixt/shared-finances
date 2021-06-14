import { Injectable } from '@angular/core';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { EmptyObject } from 'apollo-angular/types';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { Category } from '../models';

const USER_CATEGORY_CREATED_SUBSCRIPTION = gql`
  subscription userCategoryCreated {
    userCategoryCreated {
      id
      name
      color
    }
  }
`;

const USER_CATEGORY_UPDATED_SUBSCRIPTION = gql`
  subscription userCategoryUpdated {
    userCategoryUpdated {
      id
      name
      color
    }
  }
`;

const USER_CATEGORY_DELETED_SUBSCRIPTION = gql`
  subscription userCategoryDeleted {
    userCategoryDeleted {
      id
    }
  }
`;

@Injectable({
  providedIn: 'root',
})
export class UserCategoryService {
  private categoriesQueryRef: QueryRef<
    {
      categories: Category[];
    },
    EmptyObject
  >;

  constructor(private apollo: Apollo) {}

  watchCategories(): Observable<Category[]> {
    this.categoriesQueryRef = this.apollo.watchQuery<{ categories: Category[] }>({
      query: gql`
        query categories {
          categories {
            id
            name
            color
          }
        }
      `,
    });

    this.subscribeToMoreCategories();

    return this.categoriesQueryRef.valueChanges.pipe(map(result => result.data.categories));
  }

  newCategory(category: Partial<Category>): Observable<Category> {
    return this.apollo
      .mutate<{ newCategory: Category }>({
        mutation: gql`
          mutation ($name: String!, $color: String!) {
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
          mutation ($id: String!, $name: String!, $color: String!) {
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
          mutation ($categoryId: String!) {
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

  private subscribeToMoreCategories() {
    this.categoriesQueryRef.subscribeToMore({
      document: USER_CATEGORY_CREATED_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) => {
        const categories = prev.categories ?? [];

        prev = {
          categories: [...categories, subscriptionData.data.userCategoryCreated],
        };

        return {
          ...prev,
        };
      },
    });

    this.categoriesQueryRef.subscribeToMore({
      document: USER_CATEGORY_UPDATED_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) => {
        const editedCategory = subscriptionData.data.userCategoryUpdated;

        prev = {
          categories: [...prev.categories.filter(category => category.id !== editedCategory.id), editedCategory],
        };

        return {
          ...prev,
        };
      },
    });

    this.categoriesQueryRef.subscribeToMore({
      document: USER_CATEGORY_DELETED_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) => {
        prev = {
          categories: prev.categories.filter(category => category.id !== subscriptionData.data.userCategoryDeleted.id),
        };

        return {
          ...prev,
        };
      },
    });
  }
}
