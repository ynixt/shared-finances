import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  EditFinancialGoalRequestDto,
  EditGoalLedgerMovementRequestDto,
  EditGoalScheduleRequestDto,
  FinancialGoalDetailDto,
  FinancialGoalSummaryDto,
  GoalAllocateRequestDto,
  GoalContributionScheduleDto,
  GoalLedgerMovementDto,
  GoalReverseRequestDto,
  NewFinancialGoalRequestDto,
  NewGoalScheduleRequestDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/goals';
import { Page, PageRequest } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class FinancialGoalService {
  constructor(
    private httpClient: HttpClient,
    private paginationService: PaginationService,
    private userService: UserService,
  ) {}

  private async ensureUserLoaded() {
    const user = await this.userService.getUser();
    if (user == null) throw new UserMissingError();
  }

  async listIndividualGoals(pageRequest?: PageRequest): Promise<Page<FinancialGoalSummaryDto>> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.paginationService.get<FinancialGoalSummaryDto>('/api/financial-goals', pageRequest).pipe(take(1)));
  }

  async listGroupGoals(groupId: string, pageRequest?: PageRequest): Promise<Page<FinancialGoalSummaryDto>> {
    await this.ensureUserLoaded();
    return lastValueFrom(
      this.paginationService.get<FinancialGoalSummaryDto>(`/api/groups/${groupId}/financial-goals`, pageRequest).pipe(take(1)),
    );
  }

  async getGoal(goalId: string): Promise<FinancialGoalDetailDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.httpClient.get<FinancialGoalDetailDto>(`/api/financial-goals/${goalId}`).pipe(take(1)));
  }

  async createIndividualGoal(body: NewFinancialGoalRequestDto): Promise<FinancialGoalSummaryDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.httpClient.post<FinancialGoalSummaryDto>('/api/financial-goals', body).pipe(take(1)));
  }

  async createGroupGoal(groupId: string, body: NewFinancialGoalRequestDto): Promise<FinancialGoalSummaryDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.httpClient.post<FinancialGoalSummaryDto>(`/api/groups/${groupId}/financial-goals`, body).pipe(take(1)));
  }

  async updateGoal(goalId: string, body: EditFinancialGoalRequestDto): Promise<FinancialGoalSummaryDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.httpClient.patch<FinancialGoalSummaryDto>(`/api/financial-goals/${goalId}`, body).pipe(take(1)));
  }

  async deleteGoal(goalId: string): Promise<void> {
    await this.ensureUserLoaded();
    await lastValueFrom(this.httpClient.delete<void>(`/api/financial-goals/${goalId}`).pipe(take(1)));
  }

  async getLedgerMovement(goalId: string, movementId: string): Promise<GoalLedgerMovementDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(
      this.httpClient.get<GoalLedgerMovementDto>(`/api/financial-goals/${goalId}/ledger/movements/${movementId}`).pipe(take(1)),
    );
  }

  async listMovements(goalId: string, pageRequest?: PageRequest): Promise<Page<GoalLedgerMovementDto>> {
    await this.ensureUserLoaded();
    return lastValueFrom(
      this.paginationService.get<GoalLedgerMovementDto>(`/api/financial-goals/${goalId}/ledger/movements`, pageRequest).pipe(take(1)),
    );
  }

  async allocate(goalId: string, body: GoalAllocateRequestDto): Promise<void> {
    await this.ensureUserLoaded();
    await lastValueFrom(this.httpClient.post<void>(`/api/financial-goals/${goalId}/ledger/allocations`, body).pipe(take(1)));
  }

  async reverse(goalId: string, body: GoalReverseRequestDto): Promise<void> {
    await this.ensureUserLoaded();
    await lastValueFrom(this.httpClient.post<void>(`/api/financial-goals/${goalId}/ledger/reversals`, body).pipe(take(1)));
  }

  async editMovement(goalId: string, movementId: string, body: EditGoalLedgerMovementRequestDto): Promise<void> {
    await this.ensureUserLoaded();
    await lastValueFrom(this.httpClient.patch<void>(`/api/financial-goals/${goalId}/ledger/movements/${movementId}`, body).pipe(take(1)));
  }

  async deleteMovement(goalId: string, movementId: string): Promise<void> {
    await this.ensureUserLoaded();
    await lastValueFrom(this.httpClient.delete<void>(`/api/financial-goals/${goalId}/ledger/movements/${movementId}`).pipe(take(1)));
  }

  async createSchedule(goalId: string, body: NewGoalScheduleRequestDto): Promise<GoalContributionScheduleDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.httpClient.post<GoalContributionScheduleDto>(`/api/financial-goals/${goalId}/schedules`, body).pipe(take(1)));
  }

  async getSchedule(goalId: string, scheduleId: string): Promise<GoalContributionScheduleDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(
      this.httpClient.get<GoalContributionScheduleDto>(`/api/financial-goals/${goalId}/schedules/${scheduleId}`).pipe(take(1)),
    );
  }

  async listSchedules(goalId: string, pageRequest?: PageRequest): Promise<Page<GoalContributionScheduleDto>> {
    await this.ensureUserLoaded();
    return lastValueFrom(
      this.paginationService.get<GoalContributionScheduleDto>(`/api/financial-goals/${goalId}/schedules`, pageRequest).pipe(take(1)),
    );
  }

  async updateSchedule(goalId: string, scheduleId: string, body: EditGoalScheduleRequestDto): Promise<GoalContributionScheduleDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(
      this.httpClient.patch<GoalContributionScheduleDto>(`/api/financial-goals/${goalId}/schedules/${scheduleId}`, body).pipe(take(1)),
    );
  }

  async deleteSchedule(goalId: string, scheduleId: string): Promise<void> {
    await this.ensureUserLoaded();
    await lastValueFrom(this.httpClient.delete<void>(`/api/financial-goals/${goalId}/schedules/${scheduleId}`).pipe(take(1)));
  }
}
