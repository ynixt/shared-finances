import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { CursorPage, CursorPageRequest } from '../../../models/cursor-pagination';
import {
  DeleteScheduledEntryDto,
  EditScheduledEntryDto,
  EntrySummaryDto,
  EventForListDto,
  ListEntryRequestDto,
  NewEntryDto,
  ScheduledExecutionManagerRequestDto,
  SummaryEntryRequestDto,
  TransferQuoteDto,
  TransferQuoteRequestDto,
  TransferRateDto,
  TransferRateRequestDto,
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

  async editWalletEntry(id: string, request: NewEntryDto): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.put<void>(`/api/wallet-entries/${id}`, request).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }

  async editScheduledEntry(recurrenceConfigId: string, request: EditScheduledEntryDto): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.put<void>(`/api/wallet-entries/scheduled/${recurrenceConfigId}`, request).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }

  async quoteTransfer(request: TransferQuoteRequestDto): Promise<TransferQuoteDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.post<TransferQuoteDto>(`/api/wallet-entries/transfer-quote`, request).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async fetchTransferRate(request: TransferRateRequestDto): Promise<TransferRateDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.post<TransferRateDto>(`/api/wallet-entries/transfer-rate`, request).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async deleteWalletEntry(id: string): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.delete<void>(`/api/wallet-entries/${id}`).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }

  async deleteScheduledEntry(recurrenceConfigId: string, request: DeleteScheduledEntryDto): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.delete<void>(`/api/wallet-entries/scheduled/${recurrenceConfigId}`, { body: request }).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }

  async getWalletEntryById(id: string): Promise<EventForListDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.get<EventForListDto>(`/api/wallet-entries/${id}`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async getScheduledEntryByRecurrenceConfigId(recurrenceConfigId: string): Promise<EventForListDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.get<EventForListDto>(`/api/wallet-entries/scheduled/${recurrenceConfigId}`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async listWalletEntries(pageRequest?: CursorPageRequest, request?: ListEntryRequestDto): Promise<CursorPage<EventForListDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.cursorPaginationService.post<EventForListDto>(`/api/wallet-entries/list`, pageRequest, undefined, request).pipe(take(1)),
      );
    }

    throw new UserMissingError();
  }

  async listScheduledExecutions(request?: ScheduledExecutionManagerRequestDto): Promise<EventForListDto[]> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.post<EventForListDto[]>(`/api/wallet-entries/scheduled-executions/list`, request ?? {}).pipe(take(1)));
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
