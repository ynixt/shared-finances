import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  CategoryDto,
  EditCategoryDto,
  NewCategoryDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { Page, PageRequest } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

export type GetAllCategoriesParams = {
  onlyRoot?: boolean;
  mountChildren?: boolean;
  query?: string;
};

export type GetCategoryParams = {
  mountChildren?: boolean;
};

@Injectable({ providedIn: 'root' })
export class CategoriesService {
  constructor(
    private httpClient: HttpClient,
    private paginationService: PaginationService,
    private userService: UserService,
  ) {}

  async getAllCategories(params?: GetAllCategoriesParams, request?: PageRequest): Promise<Page<CategoryDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.paginationService.get<CategoryDto>('/api/categories', request, params).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async getCategory(id: string, params?: GetCategoryParams): Promise<CategoryDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.get<CategoryDto>(`/api/categories/${id}`, { params }).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async newCategory(body: NewCategoryDto): Promise<CategoryDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.post<CategoryDto>('/api/categories', body).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async editCategory(id: string, body: EditCategoryDto): Promise<CategoryDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.put<CategoryDto>(`/api/categories/${id}`, body).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async deleteCategory(id: string): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.httpClient.delete<CategoryDto>(`/api/categories/${id}`).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }
}
