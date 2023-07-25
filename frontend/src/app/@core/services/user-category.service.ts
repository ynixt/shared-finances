import { Injectable } from "@angular/core";
import { Apollo, gql, QueryRef } from "apollo-angular";
import { EmptyObject } from "apollo-angular/types";
import { lastValueFrom, Observable } from "rxjs";
import { map, take } from "rxjs/operators";
import { GenericCategoryService, GroupWithIdName } from "src/app/components/category";

import { Category, CreditCard } from "../models";
import { StompService } from "./stomp.service";
import { HttpClient } from "@angular/common/http";

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
  providedIn: "root"
})
export class UserCategoryService extends GenericCategoryService {
  private categoriesQueryRef: QueryRef<
    {
      categories: Category[];
    },
    EmptyObject
  >;

  constructor(private apollo: Apollo, private stompService: StompService, private httpClient: HttpClient) {
    super();
  }

  watchCategories(): Observable<Category[]> {
    const w = this.stompService.watch({
      destination: "/user/queue/user-transaction-category"
    });

    this.stompService.publish({ destination: "/app/user-transaction-category" });

    return w.pipe(map(message => JSON.parse(message.body) as Category[]));
  }

  newCategory(category: Partial<Category>): Observable<Category> {
    return this.httpClient.post<Category>("/api/user-transaction-category", {
      name: category.name,
      color: category.color
    });
  }

  editCategory(category: Category): Observable<Category> {
    return this.httpClient.put<Category>(`/api/user-transaction-category/${category.id}`, {
      name: category.name,
      color: category.color
    });
  }

  getById(categoryId: string): Promise<Category> {
    return lastValueFrom(this.httpClient.get<Category>(
      `/api/user-transaction-category/${categoryId}`).pipe(take(1))
    );
  }

  deleteCategory(categoryId: string): Observable<void> {
    return this.httpClient.delete<void>(
      `/api/user-transaction-category/${categoryId}`);
  }

  async getGroup(groupId: string): Promise<GroupWithIdName | null> {
    return null;
  }

  private subscribeToMoreCategories() {
    // this.categoriesQueryRef.subscribeToMore({
    //   document: USER_CATEGORY_CREATED_SUBSCRIPTION,
    //   updateQuery: (prev, { subscriptionData }) => {
    //     const categories = prev.categories ?? [];
    //
    //     prev = {
    //       categories: [...categories, subscriptionData.data.userCategoryCreated],
    //     };
    //
    //     return {
    //       ...prev,
    //     };
    //   },
    // });
    //
    // this.categoriesQueryRef.subscribeToMore({
    //   document: USER_CATEGORY_UPDATED_SUBSCRIPTION,
    //   updateQuery: (prev, { subscriptionData }) => {
    //     const editedCategory = subscriptionData.data.userCategoryUpdated;
    //
    //     prev = {
    //       categories: [...prev.categories.filter(category => category.id !== editedCategory.id), editedCategory],
    //     };
    //
    //     return {
    //       ...prev,
    //     };
    //   },
    // });
    //
    // this.categoriesQueryRef.subscribeToMore({
    //   document: USER_CATEGORY_DELETED_SUBSCRIPTION,
    //   updateQuery: (prev, { subscriptionData }) => {
    //     prev = {
    //       categories: prev.categories.filter(category => category.id !== subscriptionData.data.userCategoryDeleted.id),
    //     };
    //
    //     return {
    //       ...prev,
    //     };
    //   },
    // });
  }
}
