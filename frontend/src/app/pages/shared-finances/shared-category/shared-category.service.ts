import { Injectable } from '@angular/core';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { EmptyObject } from 'apollo-angular/types';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { Category } from 'src/app/@core/models';
import { GenericCategoryService, GroupWithIdName } from 'src/app/components/category';

const GROUP_CATEGORY_CREATED_SUBSCRIPTION = gql`
  subscription groupCategoryCreated {
    groupCategoryCreated {
      id
      name
      color
    }
  }
`;

const GROUP_CATEGORY_UPDATED_SUBSCRIPTION = gql`
  subscription groupCategoryUpdated {
    groupCategoryUpdated {
      id
      name
      color
    }
  }
`;

const GROUP_CATEGORY_DELETED_SUBSCRIPTION = gql`
  subscription groupCategoryDeleted {
    groupCategoryDeleted {
      id
    }
  }
`;

@Injectable({
  providedIn: 'root',
})
export class SharedCategoryService extends GenericCategoryService {
  private categoriesQueryRef: QueryRef<
    {
      categories: Category[];
    },
    EmptyObject
  >;

  constructor(private apollo: Apollo) {
    super();
  }

  watchCategories(groupId: string): Observable<Category[]> {
    this.categoriesQueryRef = this.apollo.watchQuery<{ categories: Category[] }>({
      query: gql`
        query categories($groupId: String!) {
          categories(groupId: $groupId) {
            id
            name
            color
          }
        }
      `,
      variables: {
        groupId,
      },
    });

    this.subscribeToMoreCategories();

    return this.categoriesQueryRef.valueChanges.pipe(map(result => result.data.categories));
  }

  newCategory(category: Partial<Category>, groupId: string): Observable<Category> {
    return this.apollo
      .mutate<{ newGroupCategory: Category }>({
        mutation: gql`
          mutation($name: String!, $color: String!, $groupId: String!) {
            newGroupCategory(name: $name, color: $color, groupId: $groupId) {
              id
              name
              color
            }
          }
        `,
        variables: {
          name: category.name,
          color: category.color,
          groupId,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.newGroupCategory),
      );
  }

  editCategory(category: Category): Observable<Category> {
    return this.apollo
      .mutate<{ editGroupCategory: Category }>({
        mutation: gql`
          mutation($id: String!, $name: String!, $color: String!) {
            editGroupCategory(id: $id, name: $name, color: $color) {
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
        map(result => result.data.editGroupCategory),
      );
  }

  getById(categoryId: string, groupId: string): Promise<Category> {
    return this.apollo
      .query<{ category: Category }>({
        query: gql`
          query category($categoryId: String!, $groupId: String!) {
            category(categoryId: $categoryId, groupId: $groupId) {
              id
              name
              color
              group {
                id
                name
              }
            }
          }
        `,
        variables: {
          categoryId,
          groupId,
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
      .mutate<{ deleteGroupCategory: boolean }>({
        mutation: gql`
          mutation($categoryId: String!) {
            deleteGroupCategory(categoryId: $categoryId)
          }
        `,
        variables: {
          categoryId,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.deleteGroupCategory),
      );
  }

  getGroup(groupId: string): Promise<GroupWithIdName | null> {
    return this.apollo
      .query<{ group: GroupWithIdName }>({
        query: gql`
          query GetGroup($groupId: String!) {
            group(groupId: $groupId) {
              id
              name
            }
          }
        `,
        variables: {
          groupId,
        },
      })
      .pipe(
        map(result => (result.errors || result.data == null || result.data.group == null ? null : result.data.group)),
        take(1),
      )
      .toPromise();
  }

  private subscribeToMoreCategories() {
    this.categoriesQueryRef.subscribeToMore({
      document: GROUP_CATEGORY_CREATED_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) => {
        const categories = prev.categories ?? [];

        prev = {
          categories: [...categories, subscriptionData.data.groupCategoryCreated],
        };

        return {
          ...prev,
        };
      },
    });

    this.categoriesQueryRef.subscribeToMore({
      document: GROUP_CATEGORY_UPDATED_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) => {
        const editedCategory = subscriptionData.data.groupCategoryUpdated;

        prev = {
          categories: [...prev.categories.filter(category => category.id !== editedCategory.id), editedCategory],
        };

        return {
          ...prev,
        };
      },
    });

    this.categoriesQueryRef.subscribeToMore({
      document: GROUP_CATEGORY_DELETED_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) => {
        prev = {
          categories: prev.categories.filter(category => category.id !== subscriptionData.data.groupCategoryDeleted.id),
        };

        return {
          ...prev,
        };
      },
    });
  }
}
