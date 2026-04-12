import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { WalletItemSearchResponseDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { Page, PageRequest } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({ providedIn: 'root' })
export class WalletItemService {
  constructor(
    private paginationService: PaginationService,
    private userService: UserService,
  ) {}

  async getAllItems(request?: PageRequest, onlyBankAccounts = false): Promise<Page<WalletItemSearchResponseDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.paginationService
          .get<WalletItemSearchResponseDto>('/api/wallet-items', request, {
            onlyBankAccounts: onlyBankAccounts ? 'true' : undefined,
          })
          .pipe(take(1)),
      );
    }

    throw new UserMissingError();
  }
}
