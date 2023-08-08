import { Injectable } from "@angular/core";
import { lastValueFrom, Observable } from "rxjs";
import { map, startWith, switchMap, take } from "rxjs/operators";
import { Category } from "src/app/@core/models";
import { GenericCategoryService, GroupWithIdName } from "src/app/components/category";
import { StompService } from "../../../@core/services/stomp.service";
import { HttpClient } from "@angular/common/http";
import { GroupsService } from "../../../@core/services";


@Injectable({
  providedIn: "root"
})
export class SharedCategoryService extends GenericCategoryService {
  constructor(private stompService: StompService, private httpClient: HttpClient, private groupService: GroupsService) {
    super();
  }

  watchCategories(groupId: string): Observable<Category[]> {
    return this.httpClient.get<Category[]>(`/api/group-transaction-category/all/${groupId}`).pipe(
      switchMap(cats => this.stompService.watch({
          destination: "/user/queue/group-transaction-category/" + groupId
        }).pipe(map(message => JSON.parse(message.body) as Category[]), startWith(cats))
      )
    );
  }

  newCategory(category: Partial<Category>, groupId: string): Observable<Category> {
    return this.httpClient.post<Category>("/api/group-transaction-category", {
      name: category.name,
      color: category.color,
      groupId
    });
  }

  editCategory(category: Category): Observable<Category> {
    return this.httpClient.put<Category>(`/api/group-transaction-category/${category.id}`, {
      name: category.name,
      color: category.color
    });
  }

  getById(categoryId: string): Promise<Category> {
    return lastValueFrom(this.httpClient.get<Category>(
      `/api/group-transaction-category/${categoryId}`).pipe(take(1))
    );
  }

  deleteCategory(categoryId: string): Observable<void> {
    return this.httpClient.delete<void>(
      `/api/group-transaction-category/${categoryId}`);
  }

  getGroup(groupId: string): Promise<GroupWithIdName | null> {
    return this.groupService.getGroupForEdit(groupId);
  }
}
