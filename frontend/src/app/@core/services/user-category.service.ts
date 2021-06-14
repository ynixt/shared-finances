import { Injectable } from '@angular/core';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { EmptyObject } from 'apollo-angular/types';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
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
