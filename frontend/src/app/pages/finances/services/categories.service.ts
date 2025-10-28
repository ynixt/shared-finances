import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { CategoryDto, NewCategoryDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { Page, PageRequest } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

export type GetAllCategoriesParams = {
  onlyRoot?: boolean;
  mountChildren?: boolean;
  query?: string;
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

  async newCategory(body: NewCategoryDto): Promise<CategoryDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.post<CategoryDto>('/api/categories', body).pipe(take(1)));
    }

    throw new UserMissingError();
  }
}
