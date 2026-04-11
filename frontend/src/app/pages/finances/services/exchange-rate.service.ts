import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { CursorPage, CursorPageRequest } from '../../../models/cursor-pagination';
import {
  ExchangeRateQuoteDto,
  ExchangeRateQuoteListRequestDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/exchangerate';
import { CursorPaginationService } from '../../../services/cursor-pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

export interface ExchangeRateQuoteListFilters {
  baseCurrency?: string | undefined;
  quoteCurrency?: string | undefined;
  quoteDateFrom?: string | undefined;
  quoteDateTo?: string | undefined;
}

@Injectable({
  providedIn: 'root',
})
export class ExchangeRateService {
  constructor(
    private cursorPaginationService: CursorPaginationService,
    private userService: UserService,
  ) {}

  async list(
    pageRequest?: CursorPageRequest | undefined,
    filters?: ExchangeRateQuoteListFilters | undefined,
  ): Promise<CursorPage<ExchangeRateQuoteDto>> {
    const user = await this.userService.getUser();

    if (user == null) {
      throw new UserMissingError();
    }

    const body: ExchangeRateQuoteListRequestDto = {
      baseCurrency: filters?.baseCurrency?.trim() || undefined,
      quoteCurrency: filters?.quoteCurrency?.trim() || undefined,
      quoteDateFrom: filters?.quoteDateFrom || undefined,
      quoteDateTo: filters?.quoteDateTo || undefined,
    };

    return lastValueFrom(
      this.cursorPaginationService.post<ExchangeRateQuoteDto>('/api/exchange-rates/list', pageRequest, undefined, body).pipe(take(1)),
    );
  }
}
