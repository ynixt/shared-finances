import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { WalletItemSearchResponseDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { WalletItemType } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { Page, PageRequest } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({ providedIn: 'root' })
export class GroupWalletItemService {
  constructor(
    private paginationService: PaginationService,
    private userService: UserService,
  ) {}

  async getAllItems(
    groupId: string,
    request?: PageRequest,
    onlyBankAccounts = false,
    query?: string,
    type?: WalletItemType,
  ): Promise<Page<WalletItemSearchResponseDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.paginationService
          .get<WalletItemSearchResponseDto>(`/api/groups/${groupId}/wallet-items`, request, {
            onlyBankAccounts: onlyBankAccounts ? 'true' : undefined,
            query: query?.trim() || undefined,
            type,
          })
          .pipe(take(1)),
      );
    }

    throw new UserMissingError();
  }
}
