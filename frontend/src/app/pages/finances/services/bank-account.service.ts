import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { BankAccountDto, NewBankAccountDto } from '../../../models/generated';
import { Page, PageRequest, Pageable } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class BankAccountService {
  constructor(
    private httpClient: HttpClient,
    private paginationService: PaginationService,
    private userService: UserService,
  ) {}

  async createBankAccount(newBankAccountDto: NewBankAccountDto): Promise<BankAccountDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.httpClient.post<BankAccountDto>('/api/bank-accounts', newBankAccountDto).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async getAllBankAccount(request?: PageRequest): Promise<Page<BankAccountDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.paginationService.get<BankAccountDto>('/api/bank-accounts', request).pipe(take(1)));
    }

    throw new UserMissingError();
  }
}
