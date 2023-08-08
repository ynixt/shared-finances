import { Injectable } from "@angular/core";
import { lastValueFrom, Observable } from "rxjs";
import { map, startWith, switchMap, take } from "rxjs/operators";
import { GenericCategoryService, GroupWithIdName } from "src/app/components/category";

import { Category } from "../models";
import { StompService } from "./stomp.service";
import { HttpClient } from "@angular/common/http";

@Injectable({
  providedIn: "root"
})
export class UserCategoryService extends GenericCategoryService {
  constructor(private stompService: StompService, private httpClient: HttpClient) {
    super();
  }

  watchCategories(): Observable<Category[]> {
    return this.httpClient.get<Category[]>(`/api/user-transaction-category`).pipe(
      switchMap(cats => this.stompService.watch({
          destination: "/user/queue/user-transaction-category"
        }).pipe(map(message => JSON.parse(message.body) as Category[]), startWith(cats))
      )
    );
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
}
