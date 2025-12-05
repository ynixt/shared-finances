import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { CursorPage, CursorPageRequest } from '../../../models/cursor-pagination';
import {
  EntryForListDto,
  EntrySummaryDto,
  ListEntryRequestDto,
  NewEntryDto,
  SummaryEntryRequestDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { CursorPaginationService } from '../../../services/cursor-pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class WalletEntryService {
  constructor(
    private http: HttpClient,
    private userService: UserService,
    private cursorPaginationService: CursorPaginationService,
  ) {}

  async createWalletEntry(newEntryRequest: NewEntryDto): Promise<any> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.post<void>(`/api/wallet-entries`, newEntryRequest).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }

  async listWalletEntries(pageRequest?: CursorPageRequest, request?: ListEntryRequestDto): Promise<CursorPage<EntryForListDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.cursorPaginationService.post<EntryForListDto>(`/api/wallet-entries/list`, pageRequest, undefined, request).pipe(take(1)),
      );
    }

    throw new UserMissingError();
  }

  async summaryWalletEntries(request?: SummaryEntryRequestDto): Promise<EntrySummaryDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.post<EntrySummaryDto>(`/api/wallet-entries/summary`, request).pipe(take(1)));
    }

    throw new UserMissingError();
  }
}
