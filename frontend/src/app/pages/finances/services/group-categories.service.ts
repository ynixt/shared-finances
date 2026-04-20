import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  CategoryConceptDto,
  CategoryDto,
  EditCategoryDto,
  NewCategoryDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { Page, PageRequest } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

export type GetAllGroupCategoriesParams = {
  onlyRoot?: boolean;
  mountChildren?: boolean;
  query?: string;
};

export type GetGroupCategoryParams = {
  mountChildren?: boolean;
};

@Injectable({ providedIn: 'root' })
export class GroupCategoriesService {
  constructor(
    private httpClient: HttpClient,
    private paginationService: PaginationService,
    private userService: UserService,
  ) {}

  async getAllCategories(groupId: string, params?: GetAllGroupCategoriesParams, request?: PageRequest): Promise<Page<CategoryDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.paginationService.get<CategoryDto>(`/api/groups/${groupId}/categories`, request, params).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async getCategory(groupId: string, id: string, params?: GetGroupCategoryParams): Promise<CategoryDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.get<CategoryDto>(`/api/groups/${groupId}/categories/${id}`, { params }).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async newCategory(groupId: string, body: NewCategoryDto): Promise<CategoryDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.post<CategoryDto>(`/api/groups/${groupId}/categories`, body).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async editCategory(groupId: string, id: string, body: EditCategoryDto): Promise<CategoryDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.put<CategoryDto>(`/api/groups/${groupId}/categories/${id}`, body).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async getAvailableConcepts(): Promise<CategoryConceptDto[]> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.get<CategoryConceptDto[]>('/api/categories/concepts').pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async deleteCategory(groupId: string, id: string): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.httpClient.delete<void>(`/api/groups/${groupId}/categories/${id}`).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }
}
