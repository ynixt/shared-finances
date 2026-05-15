import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  CreateGroupDebtAdjustmentRequestDto,
  EditGroupDebtAdjustmentRequestDto,
  GroupDebtMovementDto,
  GroupDebtPairHistoryDto,
  GroupDebtWorkspaceDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/debts';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class GroupDebtService {
  constructor(
    private http: HttpClient,
    private userService: UserService,
  ) {}

  async getWorkspace(groupId: string, selectedMonth?: string): Promise<GroupDebtWorkspaceDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      const params = selectedMonth == null ? '' : `?selectedMonth=${encodeURIComponent(selectedMonth)}`;
      return lastValueFrom(this.http.get<GroupDebtWorkspaceDto>(`/api/groups/${groupId}/debts${params}`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async listHistory(
    groupId: string,
    filter?: {
      currency?: string;
      payerId?: string;
      receiverId?: string;
      selectedMonth?: string;
    },
  ): Promise<GroupDebtMovementDto[]> {
    const user = await this.userService.getUser();

    if (user != null) {
      const searchParams = new URLSearchParams();

      if (filter?.payerId != null) {
        searchParams.set('payerId', filter.payerId);
      }
      if (filter?.receiverId != null) {
        searchParams.set('receiverId', filter.receiverId);
      }
      if (filter?.currency != null) {
        searchParams.set('currency', filter.currency);
      }
      if (filter?.selectedMonth != null) {
        searchParams.set('selectedMonth', filter.selectedMonth);
      }

      const params = searchParams.size > 0 ? `?${searchParams.toString()}` : '';
      return lastValueFrom(this.http.get<GroupDebtMovementDto[]>(`/api/groups/${groupId}/debts/history${params}`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async listPairHistory(
    groupId: string,
    filter?: {
      selectedMonth: string;
    },
  ): Promise<GroupDebtPairHistoryDto[]> {
    const user = await this.userService.getUser();

    if (user != null) {
      const searchParams = new URLSearchParams();
      if (filter?.selectedMonth != null) {
        searchParams.set('selectedMonth', filter.selectedMonth);
      }

      const params = searchParams.size > 0 ? `?${searchParams.toString()}` : '';
      return lastValueFrom(this.http.get<GroupDebtPairHistoryDto[]>(`/api/groups/${groupId}/debts/history/by-pair${params}`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async getMovement(groupId: string, movementId: string): Promise<GroupDebtMovementDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.get<GroupDebtMovementDto>(`/api/groups/${groupId}/debts/movements/${movementId}`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async createAdjustment(groupId: string, request: CreateGroupDebtAdjustmentRequestDto): Promise<GroupDebtMovementDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.post<GroupDebtMovementDto>(`/api/groups/${groupId}/debts/adjustments`, request).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async editAdjustment(groupId: string, movementId: string, request: EditGroupDebtAdjustmentRequestDto): Promise<GroupDebtMovementDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.http.put<GroupDebtMovementDto>(`/api/groups/${groupId}/debts/adjustments/${movementId}`, request).pipe(take(1)),
      );
    }

    throw new UserMissingError();
  }
}
