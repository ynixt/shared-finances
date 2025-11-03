import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  CreditCardDto,
  EditCreditCardDto,
  NewCreditCardDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { Page, PageRequest } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({ providedIn: 'root' })
export class CreditCardService {
  constructor(
    private httpClient: HttpClient,
    private paginationService: PaginationService,
    private userService: UserService,
  ) {}

  async createCreditCard(dto: NewCreditCardDto): Promise<CreditCardDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.post<CreditCardDto>('/api/credit-cards', dto).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async getAllCreditCards(request?: PageRequest): Promise<Page<CreditCardDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.paginationService.get<CreditCardDto>('/api/credit-cards', request).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async getCreditCard(id: string): Promise<CreditCardDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.get<CreditCardDto>(`/api/credit-cards/${id}`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async editCreditCard(id: string, dto: EditCreditCardDto): Promise<CreditCardDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.put<CreditCardDto>(`/api/credit-cards/${id}`, dto).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async deleteCreditCard(id: string): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.httpClient.delete(`/api/credit-cards/${id}`).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }
}
